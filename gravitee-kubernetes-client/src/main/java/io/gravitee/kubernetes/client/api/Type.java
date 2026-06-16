/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.kubernetes.client.api;

import io.gravitee.kubernetes.client.model.v1.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum Type {
    ENDPOINTS("/api/v1", "endpoints", Endpoints.class, EndpointsList.class, EndpointsEvent.class),
    ENDPOINTSLICES("/apis/discovery.k8s.io/v1", "endpointslices", EndpointSlice.class, EndpointSliceList.class, EndpointSliceEvent.class),
    SECRETS("/api/v1", "secrets", Secret.class, SecretList.class, SecretEvent.class),
    CONFIGMAPS("/api/v1", "configmaps", ConfigMap.class, ConfigMapList.class, ConfigMapEvent.class);

    private final String apiBase;
    private final String value;
    private final Class<?> clazz;
    private final Class<?> listType;
    private final Class<? extends Event<Watchable>> eventType;

    Type(String apiBase, String value, Class<?> clazz, Class<?> listType, Class<? extends Event<?>> eventType) {
        this.apiBase = apiBase;
        this.value = value;
        this.clazz = clazz;
        this.listType = listType;
        this.eventType = (Class<? extends Event<Watchable>>) eventType;
    }

    public String apiBase() {
        return apiBase;
    }

    public String value() {
        return value;
    }

    public Class<?> type() {
        return clazz;
    }

    public Class<?> listType() {
        return listType;
    }

    public Class<? extends Event<Watchable>> eventType() {
        return eventType;
    }
}
