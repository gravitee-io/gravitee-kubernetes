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
package io.gravitee.gateway.services.kube.crds.resources;

import io.gravitee.gateway.services.kube.crds.resources.plugin.Plugin;
import io.gravitee.gateway.services.kube.crds.resources.service.BackendConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayResourceSpec {

    private Map<String, Plugin> resources = new HashMap<>();
    private List<PluginReference> resourceReferences = new ArrayList<>();
    private PluginReference authenticationReference;
    private Plugin authentication;
    private BackendConfiguration defaultBackendConfigurations;

    public GatewayResourceSpec() {}

    public Map<String, Plugin> getResources() {
        return resources;
    }

    public void setResources(Map<String, Plugin> resources) {
        this.resources = resources;
    }

    public List<PluginReference> getResourceReferences() {
        return resourceReferences;
    }

    public void setResourceReferences(List<PluginReference> resourceReferences) {
        this.resourceReferences = resourceReferences;
    }

    public PluginReference getAuthenticationReference() {
        return authenticationReference;
    }

    public void setAuthenticationReference(PluginReference authenticationReference) {
        this.authenticationReference = authenticationReference;
    }

    public Plugin getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Plugin authentication) {
        this.authentication = authentication;
    }

    public BackendConfiguration getDefaultBackendConfigurations() {
        return defaultBackendConfigurations;
    }

    public void setDefaultBackendConfigurations(BackendConfiguration defaultBackendConfigurations) {
        this.defaultBackendConfigurations = defaultBackendConfigurations;
    }

    @Override
    public String toString() {
        return "GraviteeGatewaySpec{resources=" + resources + ", authentication=" + authentication + '}';
    }
}
