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
package io.gravitee.gateway.services.kube.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ObjectMapperHelper {

    private static final ObjectMapper jsonMapper;
    private static final ObjectMapper yaml;

    static {
        SimpleModule simpleModule = Fabric8sMapperUtils.initializeJsonModule();
        jsonMapper = new ObjectMapper();
        yaml = new ObjectMapper(new YAMLFactory());
        jsonMapper.registerModule(simpleModule);
        yaml.registerModule(simpleModule);
    }

    public static <T> T readYamlAs(String file, Class<T> type) {
        try {
            final InputStream resourceAsStream = ObjectMapperHelper.class.getResourceAsStream(file);
            return yaml.readValue(resourceAsStream, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
