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
package io.gravitee.gateway.services.kube.webhook;

import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.gravitee.gateway.services.kube.KubeSyncTestConfig;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = KubeSyncTestConfig.class)
public class AdmissionReviewTest {

    @Test
    public void readAdmissionReview_Gateway() throws Exception {
        ObjectMapper mapper = Serialization.jsonMapper();
        KubernetesDeserializer.registerCustomKind("gravitee.io/v1beta1", "GraviteeGateway", GatewayResource.class);
        assertNotNull(
            mapper.readValue(this.getClass().getResourceAsStream("/admission-review/gateway-update.json"), AdmissionReview.class)
        );
    }

    @Test
    public void readAdmissionReview_Plugin() throws Exception {
        ObjectMapper mapper = Serialization.jsonMapper();
        KubernetesDeserializer.registerCustomKind("gravitee.io/v1beta1", "GraviteePlugins", PluginResource.class);
        assertNotNull(
            mapper.readValue(this.getClass().getResourceAsStream("/admission-review/plugins-create.json"), AdmissionReview.class)
        );
    }
}
