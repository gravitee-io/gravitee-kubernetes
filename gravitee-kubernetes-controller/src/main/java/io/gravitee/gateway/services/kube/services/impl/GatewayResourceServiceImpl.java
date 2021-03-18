/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.kube.services.impl;

import static io.gravitee.gateway.services.kube.crds.ResourceConstants.*;
import static io.gravitee.gateway.services.kube.utils.ControllerDigestHelper.computeGenericConfigHashCode;
import static io.gravitee.gateway.services.kube.utils.K8SResourceUtils.getFullName;
import static io.reactivex.Flowable.just;
import static java.util.Collections.emptyMap;

import com.google.common.collect.Maps;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.services.kube.crds.cache.GatewayCacheManager;
import io.gravitee.gateway.services.kube.crds.cache.PluginCacheManager;
import io.gravitee.gateway.services.kube.crds.cache.PluginRevision;
import io.gravitee.gateway.services.kube.crds.resources.*;
import io.gravitee.gateway.services.kube.crds.resources.plugin.Plugin;
import io.gravitee.gateway.services.kube.crds.resources.service.BackendConfiguration;
import io.gravitee.gateway.services.kube.crds.status.GatewayResourceStatus;
import io.gravitee.gateway.services.kube.crds.status.IntegrationState;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.exceptions.ValidationException;
import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.listeners.GatewayResourceListener;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GatewayResourceServiceImpl
    extends AbstractServiceImpl<GatewayResource, GatewayResourceList, DoneableGatewayResource>
    implements GatewayResourceService, InitializingBean {

    private final List<GatewayResourceListener> listeners = new ArrayList<>();

    @Autowired
    private PluginsResourceService pluginsResourceService;

    @Autowired
    private PluginCacheManager pluginCacheManager;

    @Autowired
    private GatewayCacheManager gatewayCacheManager;

    private void initializeGatewayResourceClient(KubernetesClient client) {
        CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
            .withGroup(GROUP)
            .withVersion(DEFAULT_VERSION)
            .withScope(SCOPE)
            .withName(GATEWAY_FULLNAME)
            .withPlural(GATEWAY_PLURAL)
            .withKind(GATEWAY_KIND)
            .build();

        this.crdClient = client.customResources(context, GatewayResource.class, GatewayResourceList.class, DoneableGatewayResource.class);

        KubernetesDeserializer.registerCustomKind(GROUP + '/' + DEFAULT_VERSION, GATEWAY_KIND, GatewayResource.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initializeGatewayResourceClient(client);
    }

    @Override
    public void registerListener(GatewayResourceListener listener) {
        if (listener != null) {
            LOGGER.debug("Addition of {} as GatewayListener", listener.getClass().getName());
            this.listeners.add(listener);
        }
    }

    public List<GatewayResource> listAllGateways() {
        GatewayResourceList list = crdClient.list();
        return list.getItems();
    }

    @Override
    public GatewayResource lookup(WatchActionContext<?> context, GatewayResourceReference ref) {
        final String namespace = getReferenceNamespace(context.getNamespace(), ref);
        try {
            GatewayResource gw = this.crdClient.inNamespace(namespace).withName(ref.getName()).get();
            if (gw == null) {
                throw new PipelineException(
                    context,
                    "Gateway Reference '" + ref.getName() + "' undefined in namespace '" + namespace + "'"
                );
            }
            return gw;
        } catch (KubernetesClientException e) {
            throw new PipelineException(
                context,
                "Gateway Reference '" +
                ref.getName() +
                "' can't be read in namespace '" +
                namespace +
                "' (status=" +
                e.getStatus().getStatus() +
                "/" +
                e.getStatus().getMessage() +
                ")"
            );
        }
    }

    @Override
    public Flowable<WatchActionContext<GatewayResource>> processAction(WatchActionContext<GatewayResource> context) {
        Flowable<WatchActionContext<GatewayResource>> pipeline = just(context);
        switch (context.getEvent()) {
            case ADDED:
            case MODIFIED:
                pipeline =
                    validateGatewayResource(pipeline.filter(this::needProcessing)).map(this::notifyListeners).map(this::persistAsSuccess);
                break;
            case REFERENCE_UPDATED:
                pipeline = validateGatewayResource(pipeline).map(this::notifyListeners).map(this::persistAsSuccess);
                break;
            case DELETED:
                pipeline = pipeline.map(this::clearPluginCache).map(this::notifyListenersOnDelete);
        }
        return pipeline;
    }

    protected WatchActionContext<GatewayResource> clearPluginCache(WatchActionContext<GatewayResource> context) {
        LOGGER.debug("remove entries in plugin cache for '{}' gateway", context.getResourceName());
        // Remove all plugins reference used by this Gateway from the plugin cache
        pluginCacheManager.removePluginsUsedBy(context.getResourceFullName());
        return context;
    }

    protected Flowable<WatchActionContext<GatewayResource>> validateGatewayResource(
        Flowable<WatchActionContext<GatewayResource>> pipeline
    ) {
        return pipeline.map(this::computeBackendConfigHashCode).map(this::validateAuthentication).map(this::validateResources);
    }

    protected WatchActionContext<GatewayResource> validateAuthentication(WatchActionContext<GatewayResource> context) {
        LOGGER.debug("Validate and Compute HashCode for authentication plugin of GraviteeGateway '{}'", context.getResourceName());
        GatewayResourceSpec spec = context.getResource().getSpec();
        if (spec.getAuthentication() != null) {
            PluginRevision<Policy> policy = pluginsResourceService.buildPolicy(
                context,
                spec.getAuthentication(),
                convertToRef(context, "authentication")
            );
            context.getPluginRevisions().add(policy);
        } else if (spec.getAuthenticationReference() != null) {
            PluginRevision<Policy> policy = pluginsResourceService.buildPolicy(context, null, spec.getAuthenticationReference());
            context.getPluginRevisions().add(policy);
        }
        return context;
    }

    protected WatchActionContext<GatewayResource> validateResources(WatchActionContext<GatewayResource> context) {
        LOGGER.debug("Validate and Compute HashCode for resources of GraviteeGateway '{}'", context.getResourceName());
        extractResources(context).forEach(context.getPluginRevisions()::add);
        return context;
    }

    @Override
    public List<PluginRevision<Resource>> extractResources(WatchActionContext<GatewayResource> context) {
        List<PluginRevision<Resource>> accumulator = new ArrayList<>();
        GatewayResourceSpec spec = context.getResource().getSpec();
        if (spec.getResourceReferences() != null) {
            for (PluginReference pluginRef : spec.getResourceReferences()) {
                PluginRevision<Resource> resource = pluginsResourceService.buildResource(context, null, pluginRef);
                accumulator.add(resource);
            }
        }
        if (spec.getResources() != null) {
            for (Map.Entry<String, Plugin> pluginEntry : spec.getResources().entrySet()) {
                pluginEntry.getValue().setIdentifier(pluginEntry.getKey()); // use the identifier field to initialize resource name
                PluginRevision<Resource> resource = pluginsResourceService.buildResource(
                    context,
                    pluginEntry.getValue(),
                    convertToRef(context, pluginEntry.getKey())
                );
                accumulator.add(resource);
            }
        }
        return accumulator;
    }

    protected WatchActionContext<GatewayResource> computeBackendConfigHashCode(WatchActionContext<GatewayResource> context) {
        LOGGER.debug("Compute HashCode for DefaultBackendConfiguration of GraviteeGateway '{}'", context.getResourceName());
        GatewayResourceSpec spec = context.getResource().getSpec();
        // try to resolve secret before hash processing
        BackendConfiguration backendConfig = spec.getDefaultBackendConfigurations();
        Map<String, Object> sslOptions = backendConfig == null
            ? emptyMap()
            : kubernetesService.resolveSecret(context, context.getNamespace(), backendConfig.getHttpClientSslOptions());
        Map<String, Object> proxyOptions = backendConfig == null
            ? emptyMap()
            : kubernetesService.resolveSecret(context, context.getNamespace(), backendConfig.getHttpProxy());
        String hashCode = computeGenericConfigHashCode(backendConfig.getHttpClientOptions(), sslOptions, proxyOptions);
        context.setHttpConfigHashCode(hashCode);
        return context;
    }

    protected WatchActionContext<GatewayResource> notifyListeners(WatchActionContext<GatewayResource> context) {
        GatewayResourceStatus status = context.getResource().getStatus();
        if (status == null) {
            status = new GatewayResourceStatus();
            context.getResource().setStatus(status);
        }
        Map<String, String> newHashCodes = buildHashCodes(context);
        if (hasChanged(context, status, newHashCodes)) {
            for (GatewayResourceListener listener : this.listeners) {
                listener.onGatewayUpdate(context);
            }
        }
        return context;
    }

    protected WatchActionContext<GatewayResource> notifyListenersOnDelete(WatchActionContext<GatewayResource> context) {
        for (GatewayResourceListener listener : this.listeners) {
            listener.onGatewayUpdate(context);
        }
        return context;
    }

    @Override
    public WatchActionContext<GatewayResource> persistAsSuccess(WatchActionContext<GatewayResource> context) {
        // keep in cache all plugin reference used by this Gateway
        // in order to test broken dependencies on GraviteePlugins Update
        pluginCacheManager.registerPluginsFor(
            context.getResourceFullName(),
            context.getPluginRevisions().stream().filter(PluginRevision::isRef).collect(Collectors.toList())
        );

        GatewayResourceStatus status = context.getResource().getStatus();
        if (status == null) {
            status = new GatewayResourceStatus();
            status.setHashCodes(status.new GatewayHashCodes());
            context.getResource().setStatus(status);
        }

        final IntegrationState integration = new IntegrationState();
        integration.setObservedGeneration(context.getGeneration());
        integration.setState(IntegrationState.State.SUCCESS);
        status.setIntegration(integration);
        integration.setMessage("");

        Map<String, String> newHashCodes = buildHashCodes(context);
        // updating a CR status will trigger a new MODIFIED event, we have to test
        // if some plugins changed in order stop an infinite loop
        status.getHashCodes().setPlugins(newHashCodes);
        status.getHashCodes().setBackendConfig(context.getHttpConfigHashCode());

        return updateResourceStatusOnSuccess(context);
    }

    private boolean hasChanged(
        WatchActionContext<GatewayResource> context,
        GatewayResourceStatus status,
        Map<String, String> newHashCodes
    ) {
        return (
            !Maps.difference(newHashCodes, status.getHashCodes().getPlugins()).areEqual() ||
            !context.getHttpConfigHashCode().equals(status.getHashCodes().getBackendConfig())
        );
    }

    @Override
    public WatchActionContext<GatewayResource> persistAsError(WatchActionContext<GatewayResource> context, String message) {
        GatewayResourceStatus status = context.getResource().getStatus();
        if (status == null) {
            status = new GatewayResourceStatus();
            context.getResource().setStatus(status);
        }

        final IntegrationState integration = new IntegrationState();
        integration.setObservedGeneration(context.getGeneration());
        integration.setState(IntegrationState.State.ERROR);
        integration.setMessage(message);
        status.setIntegration(integration);

        return updateResourceStatusOnError(context, integration);
    }

    @Override
    public void maybeSafelyCreated(GatewayResource gateway) {
        try {
            validateGatewayResource(just(new WatchActionContext<>(gateway, WatchActionContext.Event.ADDED))).blockingSubscribe();
        } catch (PipelineException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    @Override
    public void maybeSafelyUpdated(GatewayResource gateway) {
        try {
            validateGatewayResource(just(new WatchActionContext<>(gateway, WatchActionContext.Event.ADDED))).blockingSubscribe();
            List<String> services = this.gatewayCacheManager.getServiceByGateway(getFullName(gateway.getMetadata()));
            for (String service : new ArrayList<>(services)) { // iterate on new Array to remove entries into services
                final boolean serviceUseGatewayAuth = this.gatewayCacheManager.getCacheEntryByService(service).useGatewayAuthDefinition();
                final boolean gwWithAuthDefinition =
                    gateway.getSpec().getAuthentication() != null || gateway.getSpec().getAuthenticationReference() != null;
                if (!serviceUseGatewayAuth || gwWithAuthDefinition) {
                    services.remove(service);
                }
            }

            if (!services.isEmpty()) {
                throw new ValidationException(
                    "Authentication definition is missing but expected by some services : [" + String.join(", ", services) + "]"
                );
            }
        } catch (PipelineException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    @Override
    public void maybeSafelyDeleted(GatewayResource gateway) {
        List<String> services = this.gatewayCacheManager.getServiceByGateway(getFullName(gateway.getMetadata()));
        if (!services.isEmpty()) {
            throw new ValidationException("Gateway resource is used by some services : [" + String.join(", ", services) + "]");
        }
    }

    @Override
    protected void resetIntegrationState(IntegrationState integration, GatewayResource refreshedResource) {
        if (refreshedResource.getStatus() != null) {
            refreshedResource.getStatus().setIntegration(integration);
        }
    }

    @Override
    protected IntegrationState extractIntegrationState(GatewayResource refreshedResource) {
        if (refreshedResource.getStatus() != null) {
            return refreshedResource.getStatus().getIntegration();
        } else {
            return null;
        }
    }
}
