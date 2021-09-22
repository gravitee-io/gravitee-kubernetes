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
package io.gravitee.kubernetes.client.model.config;

import com.fasterxml.jackson.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    @JsonProperty("apiVersion")
    private String apiVersion;

    @JsonProperty("clusters")
    private List<NamedCluster> clusters = new ArrayList<>();

    @JsonProperty("contexts")
    private List<NamedContext> contexts = new ArrayList<>();

    @JsonProperty("current-context")
    private String currentContext;

    @JsonProperty("extensions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Object> extensions = new ArrayList<>();

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("preferences")
    private Preferences preferences;

    @JsonProperty("users")
    private List<NamedAuthInfo> users = new ArrayList<>();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public Config() {}

    public Config(
        String apiVersion,
        List<NamedCluster> clusters,
        List<NamedContext> contexts,
        String currentContext,
        List<Object> extensions,
        String kind,
        Preferences preferences,
        List<NamedAuthInfo> users
    ) {
        super();
        this.apiVersion = apiVersion;
        this.clusters = clusters;
        this.contexts = contexts;
        this.currentContext = currentContext;
        this.extensions = extensions;
        this.kind = kind;
        this.preferences = preferences;
        this.users = users;
    }

    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty("apiVersion")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @JsonProperty("clusters")
    public List<NamedCluster> getClusters() {
        return clusters;
    }

    @JsonProperty("clusters")
    public void setClusters(List<NamedCluster> clusters) {
        this.clusters = clusters;
    }

    @JsonProperty("contexts")
    public List<NamedContext> getContexts() {
        return contexts;
    }

    @JsonProperty("contexts")
    public void setContexts(List<NamedContext> contexts) {
        this.contexts = contexts;
    }

    @JsonProperty("current-context")
    public String getCurrentContext() {
        return currentContext;
    }

    @JsonProperty("current-context")
    public void setCurrentContext(String currentContext) {
        this.currentContext = currentContext;
    }

    @JsonProperty("extensions")
    public List<Object> getExtensions() {
        return extensions;
    }

    @JsonProperty("extensions")
    public void setExtensions(List<Object> extensions) {
        this.extensions = extensions;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("kind")
    public void setKind(String kind) {
        this.kind = kind;
    }

    @JsonProperty("preferences")
    public Preferences getPreferences() {
        return preferences;
    }

    @JsonProperty("preferences")
    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    @JsonProperty("users")
    public List<NamedAuthInfo> getUsers() {
        return users;
    }

    @JsonProperty("users")
    public void setUsers(List<NamedAuthInfo> users) {
        this.users = users;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
