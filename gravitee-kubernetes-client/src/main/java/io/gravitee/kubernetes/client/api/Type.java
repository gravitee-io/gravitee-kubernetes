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
package io.gravitee.kubernetes.client.api;

import io.gravitee.kubernetes.client.model.v1.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum Type {
    SECRETS("secrets", Secret.class, SecretList.class, SecretEvent.class),
    CONFIGMAPS("configmaps", ConfigMap.class, ConfigMapList.class, ConfigMapEvent.class);

    private final String value;
    private final Class<?> type, listType;
    private final Class<? extends Event<?>> eventType;

    Type(String value, Class<?> type, Class<?> listType, Class<? extends Event<?>> eventType) {
        this.value = value;
        this.type = type;
        this.listType = listType;
        this.eventType = eventType;
    }

    public String value() {
        return value;
    }

    public Class<?> type() {
        return type;
    }

    public Class<?> listType() {
        return listType;
    }

    public Class<? extends Event<?>> eventType() {
        return eventType;
    }
}
