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

import static io.gravitee.gateway.services.kube.crds.ResourceConstants.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.LoadBalancerType;
import io.gravitee.definition.model.ProtocolVersion;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.crds.resources.service.BackendService;
import io.gravitee.gateway.services.kube.crds.status.IntegrationState;
import java.io.IOException;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Fabric8sMapperUtils {

    public static SimpleModule initializeJsonModule() {
        SimpleModule module = new SimpleModule();

        // useful to send data to gateway
        module.addSerializer(
            Enum.class,
            new StdSerializer<Enum>(Enum.class) {
                @Override
                public void serialize(Enum value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeString(value.name().toLowerCase());
                }
            }
        );

        module.addSerializer(
            IntegrationState.State.class,
            new StdSerializer<IntegrationState.State>(IntegrationState.State.class) {
                @Override
                public void serialize(IntegrationState.State value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    jgen.writeString(value.name());
                }
            }
        );

        module.addDeserializer(
            LoadBalancerType.class,
            new StdDeserializer<LoadBalancerType>(LoadBalancerType.class) {
                @Override
                public LoadBalancerType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                    throws IOException, JsonProcessingException {
                    return LoadBalancerType.valueOf(jsonParser.getValueAsString().replaceAll("-", "_").toUpperCase());
                }
            }
        );

        module.addDeserializer(
            ProtocolVersion.class,
            new StdDeserializer<ProtocolVersion>(ProtocolVersion.class) {
                @Override
                public ProtocolVersion deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                    throws IOException, JsonProcessingException {
                    return ProtocolVersion.valueOf(jsonParser.getValueAsString().toUpperCase());
                }
            }
        );

        module.addDeserializer(
            HttpMethod.class,
            new StdDeserializer<HttpMethod>(HttpMethod.class) {
                @Override
                public HttpMethod deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                    throws IOException, JsonProcessingException {
                    return HttpMethod.valueOf(jsonParser.getValueAsString().toUpperCase());
                }
            }
        );

        module.addDeserializer(
            BackendService.BackendServiceProtocol.class,
            new StdDeserializer<BackendService.BackendServiceProtocol>(BackendService.BackendServiceProtocol.class) {
                @Override
                public BackendService.BackendServiceProtocol deserialize(
                    JsonParser jsonParser,
                    DeserializationContext deserializationContext
                )
                    throws IOException, JsonProcessingException {
                    return BackendService.BackendServiceProtocol.valueOf(jsonParser.getValueAsString().toUpperCase());
                }
            }
        );
        return module;
    }

    public static void initJsonMapper() {
        SimpleModule module = initializeJsonModule();
        Serialization.jsonMapper().registerModule(module);
        // register custom resource for deserialization
        KubernetesDeserializer.registerCustomKind(GROUP + "/" + DEFAULT_VERSION, PLUGINS_KIND, PluginResource.class);
        KubernetesDeserializer.registerCustomKind(GROUP + "/" + DEFAULT_VERSION, GATEWAY_KIND, GatewayResource.class);
        KubernetesDeserializer.registerCustomKind(GROUP + "/" + DEFAULT_VERSION, SERVICES_KIND, ServicesResource.class);
    }
}
