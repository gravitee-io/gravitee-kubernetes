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
package io.gravitee.gateway.services.kube.crds.cache;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayCacheEntry {

    private final Map<String, Boolean> serviceWithGatewayAuth = new HashMap<>();

    String gateway;

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void addService(String service, Boolean useGatewayAuth) {
        this.serviceWithGatewayAuth.put(service, useGatewayAuth);
    }

    public Boolean gatewayAuthUsedBy(String service) {
        return ofNullable(serviceWithGatewayAuth.get(service)).orElse(Boolean.FALSE);
    }

    /**
     * @return true if At least One service uses the authentication plugin defined in the Gateway resource
     */
    public boolean useGatewayAuthDefinition() {
        return this.serviceWithGatewayAuth.values().stream().filter(b -> b == Boolean.TRUE).findFirst().isPresent();
    }
}
