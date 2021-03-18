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
import static io.gravitee.gateway.services.kube.utils.ControllerDigestHelper.computePolicyHashCode;
import static io.gravitee.gateway.services.kube.utils.ControllerDigestHelper.computeResourceHashCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.services.kube.crds.cache.PluginCacheManager;
import io.gravitee.gateway.services.kube.crds.cache.PluginRevision;
import io.gravitee.gateway.services.kube.crds.resources.*;
import io.gravitee.gateway.services.kube.crds.resources.plugin.Plugin;
import io.gravitee.gateway.services.kube.crds.status.IntegrationState;
import io.gravitee.gateway.services.kube.crds.status.PluginResourceStatus;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.exceptions.ValidationException;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.listeners.PluginsResourceListener;
import io.reactivex.Flowable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PluginsResourceServiceImpl
    extends AbstractServiceImpl<PluginResource, PluginResourceList, DoneablePluginResource>
    implements PluginsResourceService, InitializingBean {

    private final List<PluginsResourceListener> listeners = new ArrayList<>();

    @Autowired
    private PluginCacheManager pluginCacheManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        initializePluginResourceClient(client);
    }

    private void initializePluginResourceClient(KubernetesClient client) {
        CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
            .withGroup(GROUP)
            .withVersion(DEFAULT_VERSION)
            .withScope(SCOPE)
            .withName(PLUGINS_FULLNAME)
            .withPlural(PLUGINS_PLURAL)
            .withKind(PLUGINS_KIND)
            .build();

        this.crdClient = client.customResources(context, PluginResource.class, PluginResourceList.class, DoneablePluginResource.class);

        KubernetesDeserializer.registerCustomKind(GROUP + "/" + DEFAULT_VERSION, PLUGINS_KIND, PluginResource.class);
    }

    @Override
    public void registerListener(PluginsResourceListener listener) {
        if (listener != null) {
            LOGGER.debug("Addition of {} as PluginsListener", listener.getClass().getName());
            this.listeners.add(listener);
        }
    }

    @Override
    public Flowable<WatchActionContext<PluginResource>> processAction(WatchActionContext<PluginResource> context) {
        Flowable<WatchActionContext<PluginResource>> pipeline = Flowable.just(context);
        switch (context.getEvent()) {
            case ADDED:
            case MODIFIED:
                pipeline = pipeline.filter(this::needProcessing).map(this::validate).map(this::notifyListeners).map(this::persistAsSuccess);
                break;
            case DELETED:
                pipeline = pipeline.map(this::notifyListenersOnDelete);
        }
        return pipeline;
    }

    protected WatchActionContext<PluginResource> validate(WatchActionContext<PluginResource> context) {
        LOGGER.debug("Validating GraviteePlugin resource '{}'", context.getResourceName());
        List<PluginRevision<?>> pluginRevisions = generatePluginRevision(context, true);
        context.addAllRevisions(pluginRevisions);
        return context;
    }

    /**
     * Parse all Plocy and Resource definition to generate list of PluginRevision.
     * If the readConf is set to false, only configuration deserialization and secret resolution are bypass. This is useful to process the CustomResource for validation by AdmissionHook
     * @param context
     * @param readConf
     * @return
     */
    private List<PluginRevision<?>> generatePluginRevision(WatchActionContext<PluginResource> context, boolean readConf) {
        PluginResourceSpec spec = context.getResource().getSpec();
        List<PluginRevision<?>> pluginRevisions = new ArrayList<>();
        for (Map.Entry<String, Plugin> entry : spec.getPlugins().entrySet()) {
            Plugin plugin = entry.getValue();
            try {
                if (plugin.defineResource()) {
                    Resource resource = new Resource();
                    resource.setName(buildResourceName(context, entry.getKey()));
                    resource.setType(plugin.getResource());
                    if (readConf) {
                        resource.setConfiguration(
                            OBJECT_MAPPER.writeValueAsString(
                                kubernetesService.resolveSecret(context, context.getNamespace(), plugin.getConfiguration())
                            )
                        );
                    }
                    PluginReference ref = convertToRef(context, entry.getKey());
                    pluginRevisions.add(new PluginRevision<>(resource, ref, context.getGeneration(), computeResourceHashCode(resource)));
                } else {
                    // policy or security policy, both have the same controls
                    final Policy policy = new Policy();
                    policy.setName(plugin.getPolicy());
                    if (readConf) {
                        policy.setConfiguration(
                            OBJECT_MAPPER.writeValueAsString(
                                kubernetesService.resolveSecret(context, context.getNamespace(), plugin.getConfiguration())
                            )
                        );
                    }
                    PluginReference ref = convertToRef(context, entry.getKey());
                    pluginRevisions.add(new PluginRevision<>(policy, ref, context.getGeneration(), computePolicyHashCode(policy)));
                }
            } catch (JsonProcessingException e) {
                LOGGER.warn("Unable to process configuration for plugin {}", entry.getKey(), e);
                throw new PipelineException(context, "Unable to convert plugin configuration", e);
            }
        }
        return pluginRevisions;
    }

    @Override
    public WatchActionContext<PluginResource> persistAsSuccess(WatchActionContext<PluginResource> context) {
        PluginResourceStatus status = context.getResource().getStatus();
        if (status == null) {
            status = new PluginResourceStatus();
            context.getResource().setStatus(status);
        }

        final IntegrationState integration = new IntegrationState();
        integration.setObservedGeneration(context.getGeneration());
        integration.setState(IntegrationState.State.SUCCESS);
        status.setIntegration(integration);
        integration.setMessage("");

        Map<String, String> newHashCodes = new HashMap<>();
        context
            .getPluginRevisions()
            .forEach(
                rev -> {
                    newHashCodes.put(rev.getPluginReference().getName(), rev.getHashCode());
                }
            );

        status.setHashCodes(newHashCodes);
        return updateResourceStatusOnSuccess(context);
    }

    private boolean hasChanged(PluginResourceStatus status, Map<String, String> newHashCodes) {
        return !Maps.difference(newHashCodes, status.getHashCodes()).areEqual();
    }

    @Override
    public WatchActionContext<PluginResource> persistAsError(WatchActionContext<PluginResource> context, String message) {
        PluginResourceStatus status = context.getResource().getStatus();
        if (status == null) {
            status = new PluginResourceStatus();
            context.getResource().setStatus(status);
        }

        final IntegrationState integration = new IntegrationState();
        integration.setObservedGeneration(context.getGeneration());
        integration.setState(IntegrationState.State.ERROR);
        integration.setMessage(message);
        status.setIntegration(integration);

        return updateResourceStatusOnError(context, integration);
    }

    private String buildResourceName(WatchActionContext<PluginResource> context, String name) {
        return name + "." + context.getResourceName() + "." + context.getNamespace();
    }

    private String buildResourceName(PluginResource pluginCustomResource, String name) {
        return name + "." + pluginCustomResource.getMetadata().getName() + "." + pluginCustomResource.getMetadata().getNamespace();
    }

    protected WatchActionContext<PluginResource> notifyListeners(WatchActionContext<PluginResource> context) {
        PluginResourceStatus status = context.getResource().getStatus();
        if (status == null) {
            status = new PluginResourceStatus();
            context.getResource().setStatus(status);
        }

        Map<String, String> newHashCodes = buildHashCodes(context);
        if (hasChanged(status, newHashCodes)) {
            for (PluginsResourceListener listener : this.listeners) {
                listener.onPluginsUpdate(context);
            }
        }
        return context;
    }

    protected WatchActionContext<PluginResource> notifyListenersOnDelete(WatchActionContext<PluginResource> context) {
        for (PluginsResourceListener listener : this.listeners) {
            listener.onPluginsUpdate(context);
        }
        return context;
    }

    @Override
    public PluginRevision<Policy> buildPolicy(WatchActionContext context, Plugin plugin, PluginReference pluginRef) {
        PluginRevision<Policy> result = new PluginRevision<>(null);
        try {
            if (plugin != null) {
                if (plugin.definePolicy()) {
                    final Policy policy = new Policy();
                    policy.setName(plugin.getPolicy());
                    policy.setConfiguration(
                        OBJECT_MAPPER.writeValueAsString(
                            kubernetesService.resolveSecret(context, context.getNamespace(), plugin.getConfiguration())
                        )
                    );
                    result = new PluginRevision<>(policy, pluginRef, context.getGeneration(), computePolicyHashCode(policy));
                }
            }

            if (pluginRef != null && !result.isValid()) {
                // if namespace isn't specified in the plugin reference, we use the same namespace as the context resource
                final String namespace = getReferenceNamespace(context, pluginRef);
                PluginResource gioPlugin = loadPluginDefinition(context, pluginRef, namespace);

                Optional<Plugin> optPlugin = gioPlugin.getSpec().getPlugin(pluginRef.getName());

                result = new PluginRevision<>(null, pluginRef, gioPlugin.getMetadata().getGeneration(), null);
                if (optPlugin.isPresent()) {
                    plugin = optPlugin.get();
                    if (plugin.definePolicy()) {
                        final Policy policy = new Policy();
                        policy.setName(plugin.getPolicy());
                        policy.setConfiguration(
                            OBJECT_MAPPER.writeValueAsString(kubernetesService.resolveSecret(context, namespace, plugin.getConfiguration()))
                        );
                        result =
                            new PluginRevision<>(policy, pluginRef, gioPlugin.getMetadata().getGeneration(), computePolicyHashCode(policy));
                    }
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to process policy configuration for plugin {}", plugin, e);
        }

        return result;
    }

    @Override
    public PluginRevision<Resource> buildResource(WatchActionContext context, Plugin plugin, PluginReference pluginRef) {
        PluginRevision<Resource> result = new PluginRevision<>(null);
        try {
            if (plugin != null) {
                if (plugin.defineResource()) {
                    final Resource resource = new Resource();
                    resource.setName(buildResourceName(context, plugin.getIdentifier()));
                    resource.setType(plugin.getResource());
                    resource.setConfiguration(
                        OBJECT_MAPPER.writeValueAsString(
                            kubernetesService.resolveSecret(context, context.getNamespace(), plugin.getConfiguration())
                        )
                    );
                    result = new PluginRevision<>(resource, pluginRef, context.getGeneration(), computeResourceHashCode(resource));
                }
            }

            if (pluginRef != null && !result.isValid()) {
                // if namespace isn't specified in the plugin reference, we use the same namespace as the context resource
                final String namespace = getReferenceNamespace(context, pluginRef);
                PluginResource gioPlugin = loadPluginDefinition(context, pluginRef, namespace);

                Optional<Plugin> optPlugin = gioPlugin.getSpec().getPlugin(pluginRef.getName());

                result = new PluginRevision<>(null, pluginRef, gioPlugin.getMetadata().getGeneration(), null);
                if (optPlugin.isPresent()) {
                    plugin = optPlugin.get();
                    if (plugin.defineResource()) {
                        Resource resource = new Resource();
                        resource.setName(buildResourceName(gioPlugin, pluginRef.getName()));
                        resource.setType(plugin.getResource());
                        resource.setConfiguration(
                            OBJECT_MAPPER.writeValueAsString(
                                kubernetesService.resolveSecret(context, context.getNamespace(), plugin.getConfiguration())
                            )
                        );

                        result = new PluginRevision<>(resource, pluginRef, context.getGeneration(), computeResourceHashCode(resource));
                    }
                }
            }
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to process resource configuration for plugin {}", plugin, e);
        }

        return result;
    }

    protected PluginResource loadPluginDefinition(WatchActionContext context, PluginReference pluginRef, String namespace) {
        try {
            PluginResource gioPlugin = this.crdClient.inNamespace(namespace).withName(pluginRef.getResource()).get();
            if (gioPlugin == null) {
                throw new PipelineException(
                    context,
                    "Reference '" + pluginRef.getResource() + "' undefined in namespace '" + namespace + "'"
                );
            }
            return gioPlugin;
        } catch (KubernetesClientException e) {
            throw new PipelineException(
                context,
                "Reference '" +
                pluginRef.getName() +
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
    public void maybeSafelyCreated(PluginResource plugin) {
        try {
            this.validate(new WatchActionContext<>(plugin, WatchActionContext.Event.ADDED));
        } catch (PipelineException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    @Override
    public void maybeSafelyUpdated(PluginResource plugin, PluginResource oldPlugin) {
        WatchActionContext<PluginResource> pluginCtx = new WatchActionContext<>(plugin, WatchActionContext.Event.MODIFIED);
        WatchActionContext<PluginResource> oldPluginCtx = new WatchActionContext<>(oldPlugin, WatchActionContext.Event.MODIFIED);

        try {
            this.validate(pluginCtx);
            List<PluginRevision<?>> oldPluginRevisions = this.generatePluginRevision(oldPluginCtx, false);
            checkBrokenPluginUsage(getDeletedPlugins(pluginCtx.getPluginRevisions(), oldPluginRevisions));
        } catch (PipelineException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /**
     * Perform a diff between both context to generate a list of PluginReference removed from the GraviteePlugin by the resource update.
     * @param pluginRevision
     * @param oldPluginRevision
     * @return
     */
    private Stream<PluginReference> getDeletedPlugins(List<PluginRevision<?>> pluginRevision, List<PluginRevision<?>> oldPluginRevision) {
        return oldPluginRevision
            .stream()
            .map(PluginRevision::getPluginReference)
            .filter(
                maybeDeleted ->
                    !pluginRevision
                        .stream()
                        .map(PluginRevision::getPluginReference)
                        .filter(present -> present.equals(maybeDeleted))
                        .findFirst()
                        .isPresent()
            );
    }

    @Override
    public void maybeSafelyDeleted(PluginResource deletedPlugin) {
        WatchActionContext<PluginResource> context = new WatchActionContext<>(deletedPlugin, WatchActionContext.Event.DELETED);
        checkBrokenPluginUsage(this.generatePluginRevision(context, false).stream().map(PluginRevision::getPluginReference));
    }

    private void checkBrokenPluginUsage(Stream<PluginReference> pluginRef) {
        List<String> resources = pluginRef
            .flatMap(ref -> pluginCacheManager.resourcesUsingPlugin(ref).stream())
            .distinct()
            .collect(Collectors.toList());
        if (!resources.isEmpty()) {
            throw new ValidationException(
                "Plugins are used by GraviteeGateway or GraviteeServices : [" + String.join(", ", resources) + "]"
            );
        }
    }

    @Override
    protected void resetIntegrationState(IntegrationState integration, PluginResource refreshedResource) {
        if (refreshedResource.getStatus() != null) {
            refreshedResource.getStatus().setIntegration(integration);
        }
    }

    @Override
    protected IntegrationState extractIntegrationState(PluginResource refreshedResource) {
        if (refreshedResource.getStatus() != null) {
            return refreshedResource.getStatus().getIntegration();
        } else {
            return null;
        }
    }
}
