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
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.kubernetes.mapper.CustomResource;
import io.gravitee.kubernetes.mapper.CustomResourceDefinitionMapper;
import io.gravitee.kubernetes.mapper.GroupVersionKind;
import io.gravitee.kubernetes.mapper.ObjectMeta;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class CustomResourceDefinitionMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CustomResourceDefinitionMapper mapper = new CustomResourceDefinitionMapper();

    @Test
    public void shouldConvertApiDefinition() throws Exception {
        JsonNode given = apiDefinition("api-definition-1.json");
        String expected = customResource("api-definition-1-.yml.expected");
        ApiResource resource = new ApiResource(objectMeta("api-definition"), given);
        String apiCustomDefinitionResource = mapper.toCustomResourceDefinition(resource);
        assertEquals(apiCustomDefinitionResource, expected);
    }

    private static ObjectMeta objectMeta(String name) {
        ObjectMeta metadata = new ObjectMeta("api-definition");
        metadata.addLabel("app.kubernetes.io/managed-by", "gravitee");
        metadata.addAnnotation("api.gravitee.io/tag", "test");
        return metadata;
    }

    private JsonNode apiDefinition(String fileName) throws IOException {
        String path = "io/gravitee/kubernetes/mapper/api/v1alpha1/management/" + fileName;
        URL resource = getClass().getClassLoader().getResource(path);
        return MAPPER.readTree(resource);
    }

    private String customResource(String fileName) throws Exception {
        String path = "io/gravitee/kubernetes/mapper/api/v1alpha1/management/" + fileName;
        URL resource = getClass().getClassLoader().getResource(path);
        return Files.readString(Paths.get(resource.toURI()));
    }

    private static class ApiResource extends CustomResource<JsonNode> {

        public ApiResource(ObjectMeta metadata, JsonNode spec) {
            super(GroupVersionKind.GIO_V1_ALPHA_1_API_DEFINITION, metadata, spec);
        }
    }
}
