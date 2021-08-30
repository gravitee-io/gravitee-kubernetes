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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigMapList {

    private String apiVersion = "v1";
    private List<ConfigMap> items = new ArrayList<>();
    private String kind = "ConfigMapList";
    private ListMeta metadata;

    /**
     * No args constructor for use in serialization
     */
    public ConfigMapList() {}

    /**
     * @param metadata
     * @param apiVersion
     * @param kind
     * @param items
     */
    public ConfigMapList(
        String apiVersion,
        List<io.gravitee.gateway.services.kube.client.model.v1.ConfigMap> items,
        String kind,
        io.gravitee.gateway.services.kube.client.model.v1.ListMeta metadata
    ) {
        super();
        this.apiVersion = apiVersion;
        this.items = items;
        this.kind = kind;
        this.metadata = metadata;
    }

    /**
     * (Required)
     */
    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * (Required)
     */
    @JsonProperty("apiVersion")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @JsonProperty("items")
    public List<io.gravitee.gateway.services.kube.client.model.v1.ConfigMap> getItems() {
        return items;
    }

    @JsonProperty("items")
    public void setItems(List<io.gravitee.gateway.services.kube.client.model.v1.ConfigMap> items) {
        this.items = items;
    }

    /**
     * (Required)
     */
    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    /**
     * (Required)
     */
    @JsonProperty("kind")
    public void setKind(String kind) {
        this.kind = kind;
    }

    @JsonProperty("metadata")
    public io.gravitee.gateway.services.kube.client.model.v1.ListMeta getMetadata() {
        return metadata;
    }

    @JsonProperty("metadata")
    public void setMetadata(io.gravitee.gateway.services.kube.client.model.v1.ListMeta metadata) {
        this.metadata = metadata;
    }
}
