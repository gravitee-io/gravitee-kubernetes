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
package io.gravitee.gateway.services.kube.crds.resources.service;

import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.services.kube.crds.resources.PluginReference;
import io.gravitee.gateway.services.kube.crds.resources.plugin.Plugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeService {

    public static final String DEFAULT_SERVICE_TYPE = "API";

    private String type = DEFAULT_SERVICE_TYPE;

    private String name;

    private boolean enabled = true;

    private PluginReference authenticationReference;

    private Plugin authentication;

    private Cors cors;

    private List<VirtualHost> vhosts = new ArrayList<>();

    private List<ServicePath> paths = new ArrayList<>();

    private Map<String, ServiceEndpoint> endpoints = new HashMap<>();

    private Map<String, Plugin> resources = new HashMap<>();

    private List<PluginReference> resourceReferences = new ArrayList<>();

    public GraviteeService() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static String getDefaultServiceType() {
        return DEFAULT_SERVICE_TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public List<VirtualHost> getVhosts() {
        return vhosts;
    }

    public void setVhosts(List<VirtualHost> vhosts) {
        this.vhosts = vhosts;
    }

    public List<ServicePath> getPaths() {
        return paths;
    }

    public void setPaths(List<ServicePath> paths) {
        this.paths = paths;
    }

    public Map<String, ServiceEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, ServiceEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    @Override
    public String toString() {
        return (
            "GraviteeService{" +
            "type='" +
            type +
            '\'' +
            ", name=" +
            name +
            ", enable=" +
            enabled +
            ", security=" +
            authenticationReference +
            ", cors=" +
            cors +
            ", vhosts=" +
            vhosts +
            ", paths=" +
            paths +
            ", endpoints=" +
            endpoints +
            '}'
        );
    }
}
