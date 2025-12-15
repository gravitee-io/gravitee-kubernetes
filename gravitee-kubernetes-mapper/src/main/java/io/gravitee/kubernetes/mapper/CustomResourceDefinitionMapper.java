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
package io.gravitee.kubernetes.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Iterator;
import java.util.Map;

public class CustomResourceDefinitionMapper {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public <T> String toCustomResourceDefinition(CustomResource<T> customResource) throws JsonProcessingException {
        if (customResource.getSpec() instanceof ObjectNode node) {
            removeNulls(node);
        }
        return objectMapper.writeValueAsString(customResource);
    }

    private void removeNulls(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode child = entry.getValue();

                if (child.isNull()) {
                    fields.remove();
                } else if (child.isContainerNode()) {
                    removeNulls(child);
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrNode = (ArrayNode) node;
            for (int i = arrNode.size() - 1; i >= 0; i--) {
                JsonNode element = arrNode.get(i);
                if (element.isNull()) {
                    arrNode.remove(i);
                } else if (element.isContainerNode()) {
                    removeNulls(element);
                }
            }
        }
    }
}
