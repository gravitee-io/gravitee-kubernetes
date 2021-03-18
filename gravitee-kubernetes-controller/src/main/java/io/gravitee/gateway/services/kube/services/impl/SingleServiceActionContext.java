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

import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.kube.crds.cache.PluginRevision;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.crds.resources.service.GraviteeService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SingleServiceActionContext extends WatchActionContext<ServicesResource> {

    private final GraviteeService serviceResource;
    private final String serviceName;

    private Api api; // TODO use the right reactable type

    /**
     * Gateway referenced by the service CustomResource
     */
    private GatewayResource gateway;
    /**
     * HttpSSLOptions of Gateway referenced by the service CustomResource
     */
    private HttpClientSslOptions gatewaySslOptions;
    /**
     * HttpProxy of Gateway referenced by the service CustomResource
     */
    private HttpProxy gatewayProxyConf;

    /**
     * use to retrieve easily if the API must be redeploy in case of plugin resource updates
     */
    private final List<PluginRevision<?>> pluginRevisions = new ArrayList<>();

    /**
     * Context Path used by the service (only one in Path base mode / more is possible in vhost mode)
     */
    private final Set<String> contextPaths = new HashSet<>();

    /**
     * Flag used to inform if the Service uses the authentication definition coming from the Gateway Spec
     */
    private boolean useGatewayAuthentication = false;

    public SingleServiceActionContext(ServiceWatchActionContext origin, GraviteeService serviceResource, String name) {
        super(origin.getResource(), origin.getEvent());
        this.serviceResource = serviceResource;
        this.serviceName = name;

        this.gateway = origin.getGateway();
        this.gatewayProxyConf = origin.getGatewayProxyConf();
        this.gatewaySslOptions = origin.getGatewaySslOptions();
    }

    public String getServiceName() {
        return serviceName;
    }

    public GraviteeService getServiceResource() {
        return serviceResource;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public void addPluginRevision(PluginRevision<?> revision) {
        this.pluginRevisions.add(revision);
    }

    public GatewayResource getGateway() {
        return gateway;
    }

    public void setGateway(GatewayResource gateway) {
        this.gateway = gateway;
    }

    public HttpClientSslOptions getGatewaySslOptions() {
        return gatewaySslOptions;
    }

    public void setGatewaySslOptions(HttpClientSslOptions gatewaySslOptions) {
        this.gatewaySslOptions = gatewaySslOptions;
    }

    public HttpProxy getGatewayProxyConf() {
        return gatewayProxyConf;
    }

    public void setGatewayProxyConf(HttpProxy gatewayProxyConf) {
        this.gatewayProxyConf = gatewayProxyConf;
    }

    public boolean isUseGatewayAuthentication() {
        return useGatewayAuthentication;
    }

    public void setUseGatewayAuthentication(boolean useGatewayAuthentication) {
        this.useGatewayAuthentication = useGatewayAuthentication;
    }

    public Set<String> getContextPaths() {
        return contextPaths;
    }

    public void addContextPath(String contextPath) {
        this.contextPaths.add(contextPath);
    }

    // -- utils methods to regroup in GSUtils class ?
    public String buildApiId() {
        return serviceName + "." + getResource().getMetadata().getName() + "." + getResource().getMetadata().getNamespace();
    }
}
