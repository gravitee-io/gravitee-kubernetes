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

import io.gravitee.definition.model.LoadBalancerType;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceEndpoint {

    private List<BackendService> backendServices;

    private LoadBalancerType loadBalancing;

    private BackendConfiguration configuration;

    public ServiceEndpoint() {}

    public LoadBalancerType getLoadBalancing() {
        return loadBalancing;
    }

    public void setLoadBalancing(LoadBalancerType loadBalancing) {
        this.loadBalancing = loadBalancing;
    }

    public List<BackendService> getBackendServices() {
        return backendServices;
    }

    public void setBackendServices(List<BackendService> backendServices) {
        this.backendServices = backendServices;
    }

    public BackendConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(BackendConfiguration configuration) {
        this.configuration = configuration;
    }
}
