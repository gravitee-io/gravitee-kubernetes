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

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BackendService {

    private BackendServiceType type = BackendServiceType.kubernetes;
    private BackendServiceProtocol protocol = BackendServiceProtocol.HTTP;
    private String namespace = "default";
    private String name;
    private int port;
    private String target;
    private int weight;
    private BackendConfiguration configuration;

    public BackendService() {}

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public BackendServiceType getType() {
        return type;
    }

    public void setType(BackendServiceType type) {
        this.type = type;
    }

    public BackendServiceProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(BackendServiceProtocol protocol) {
        this.protocol = protocol;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public BackendConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(BackendConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return (
            "BackendService{" +
            "type=" +
            type +
            ", namespace='" +
            namespace +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", port=" +
            port +
            ", target='" +
            target +
            '\'' +
            '}'
        );
    }

    public enum BackendServiceType {
        kubernetes,
        external,
    }

    public enum BackendServiceProtocol {
        HTTP,
        HTTPS,
        GRPC,
    }
}
