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
package io.gravitee.kubernetes.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.kubernetes.client.config.KubernetesConfig;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
class HttpClientConfigurationTest {

    @Test
    void should_load_a_client_with_ec_key_pair() {
        KubernetesConfig config = KubernetesConfig.newInstance("src/test/resources/config_ec.yaml");
        assertThat(config.getAccessToken()).isNull();
        KubernetesClientV1Impl kubernetesClientV1 = new KubernetesClientV1Impl(config);
        assertThatCode(kubernetesClientV1::httpClient).doesNotThrowAnyException();
    }

    @Test
    void should_load_a_client_with_rsa_key_pair() {
        KubernetesConfig config = KubernetesConfig.newInstance("src/test/resources/config.yaml");
        KubernetesClientV1Impl kubernetesClientV1 = new KubernetesClientV1Impl(config);
        assertThatCode(kubernetesClientV1::httpClient).doesNotThrowAnyException();
    }
}
