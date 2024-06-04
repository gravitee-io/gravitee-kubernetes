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
package io.gravitee.kubernetes.client.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigMap implements Serializable, Watchable {

    private String apiVersion = "v1";
    private Map<String, String> binaryData;
    private Map<String, String> data;
    private Boolean immutable;
    private String kind = "ConfigMap";
    private ObjectMeta metadata;

    public ConfigMap() {}

    /**
     *
     * @param immutable
     * @param metadata
     * @param apiVersion
     * @param data
     * @param binaryData
     * @param kind
     */
    public ConfigMap(
        String apiVersion,
        Map<String, String> binaryData,
        Map<String, String> data,
        Boolean immutable,
        String kind,
        ObjectMeta metadata
    ) {
        super();
        this.apiVersion = apiVersion;
        this.binaryData = binaryData;
        this.data = data;
        this.immutable = immutable;
        this.kind = kind;
        this.metadata = metadata;
    }

    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty("apiVersion")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @JsonProperty("binaryData")
    public Map<String, String> getBinaryData() {
        return binaryData;
    }

    @JsonProperty("binaryData")
    public void setBinaryData(Map<String, String> binaryData) {
        this.binaryData = binaryData;
    }

    @JsonProperty("data")
    public Map<String, String> getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @JsonProperty("immutable")
    public Boolean getImmutable() {
        return immutable;
    }

    @JsonProperty("immutable")
    public void setImmutable(Boolean immutable) {
        this.immutable = immutable;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("kind")
    public void setKind(String kind) {
        this.kind = kind;
    }

    @JsonProperty("metadata")
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return (
            "ConfigMap{" +
            "apiVersion='" +
            apiVersion +
            '\'' +
            ", binaryData=" +
            binaryData +
            ", data=" +
            data +
            ", immutable=" +
            immutable +
            ", kind='" +
            kind +
            '\'' +
            ", metadata=" +
            metadata +
            '}'
        );
    }

    @Override
    public ObjectMeta metaData() {
        return this.metadata;
    }
}
