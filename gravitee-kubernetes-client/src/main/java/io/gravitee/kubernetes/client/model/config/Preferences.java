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
public class Preferences {

    @JsonProperty("colors")
    private Boolean colors;

    @JsonProperty("extensions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Object> extensions = new ArrayList<>();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public Preferences() {}

    /**
     *
     * @param extensions
     * @param colors
     */
    public Preferences(Boolean colors, List<Object> extensions) {
        super();
        this.colors = colors;
        this.extensions = extensions;
    }

    @JsonProperty("colors")
    public Boolean getColors() {
        return colors;
    }

    @JsonProperty("colors")
    public void setColors(Boolean colors) {
        this.colors = colors;
    }

    @JsonProperty("extensions")
    public List<Object> getExtensions() {
        return extensions;
    }

    @JsonProperty("extensions")
    public void setExtensions(List<Object> extensions) {
        this.extensions = extensions;
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
