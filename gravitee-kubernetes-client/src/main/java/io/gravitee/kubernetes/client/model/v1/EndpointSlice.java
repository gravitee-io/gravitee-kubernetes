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
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author GraviteeSource Team
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EndpointSlice implements Serializable, Watchable {

    @Serial
    private static final long serialVersionUID = -4085686808007684134L;

    @JsonProperty("metadata")
    private ObjectMeta metadata;

    @JsonProperty("addressType")
    private String addressType;

    @JsonProperty("endpoints")
    private List<EndpointSliceEndpoint> endpoints;

    @JsonProperty("ports")
    private List<EndpointSlicePort> ports;

    @Override
    public ObjectMeta metaData() {
        return metadata;
    }
}
