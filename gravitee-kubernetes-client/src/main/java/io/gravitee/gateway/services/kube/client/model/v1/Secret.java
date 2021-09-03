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
package io.gravitee.gateway.services.kube.client.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Secret implements Serializable {

    private String apiVersion = "v1";
    private Map<String, String> data;
    private Boolean immutable;
    private String kind = "Secret";
    private ObjectMeta metadata;
    private Map<String, String> stringData;
    private String type;

    public Secret() {}

    public Secret(
        String apiVersion,
        Map<String, String> data,
        Boolean immutable,
        String kind,
        ObjectMeta metadata,
        Map<String, String> stringData,
        String type
    ) {
        this.apiVersion = apiVersion;
        this.data = data;
        this.immutable = immutable;
        this.kind = kind;
        this.metadata = metadata;
        this.stringData = stringData;
        this.type = type;
    }

    public String getApiVersion() {
        return this.apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Map<String, String> getData() {
        return this.data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public Boolean getImmutable() {
        return this.immutable;
    }

    public void setImmutable(Boolean immutable) {
        this.immutable = immutable;
    }

    public String getKind() {
        return this.kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public ObjectMeta getMetadata() {
        return this.metadata;
    }

    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> getStringData() {
        return this.stringData;
    }

    public void setStringData(Map<String, String> stringData) {
        this.stringData = stringData;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return (
            "Secret{" +
            "apiVersion='" +
            apiVersion +
            '\'' +
            ", data=" +
            data +
            ", immutable=" +
            immutable +
            ", kind='" +
            kind +
            '\'' +
            ", metadata=" +
            metadata +
            ", stringData=" +
            stringData +
            ", type='" +
            type +
            '\'' +
            '}'
        );
    }
}
