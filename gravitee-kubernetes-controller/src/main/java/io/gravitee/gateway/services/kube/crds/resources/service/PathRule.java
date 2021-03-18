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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.services.kube.crds.resources.PluginReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathRule {

    private Set<HttpMethod> methods = new HashSet<>();

    @JsonProperty("policyReference")
    private PluginReference policyRef;

    private String policy;

    private Map<String, Object> configuration;

    public PathRule() {}

    public Set<HttpMethod> getMethods() {
        return methods;
    }

    public void setMethods(Set<HttpMethod> methods) {
        this.methods = methods;
    }

    public PluginReference getPolicyRef() {
        return policyRef;
    }

    public void setPolicyRef(PluginReference policyRef) {
        this.policyRef = policyRef;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return (
            "PathRule{" +
            "methods=" +
            methods +
            ", policyRef=" +
            policyRef +
            ", policy='" +
            policy +
            '\'' +
            ", configuration=" +
            configuration +
            '}'
        );
    }
}
