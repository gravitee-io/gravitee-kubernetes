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
package io.gravitee.kubernetes.client.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KubernetesConfigTest {

    @Test
    void should_fail_to_load() {
        assertThatCode(() -> KubernetesConfig.newInstance("src/test/resources/fake"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("src/test/resources/fake");
    }

    @Test
    void should_build_config_with_file_without_namespace() {
        KubernetesConfig instance = KubernetesConfig.newInstance("src/test/resources/config.yaml");
        assertThat(instance)
            .isNotSameAs(KubernetesConfig.newInstance("src/test/resources/config.yaml"))
            .isNotSameAs(KubernetesConfig.getInstance());
        assertThat(instance.getFile().getAbsolutePath()).endsWith("src/test/resources/config.yaml");
        assertThat(instance.getCaCertData()).isNotBlank();
        assertThat(instance.getAccessToken()).isNotBlank();
        assertThat(instance.getApiServerHost()).isEqualTo("127.0.0.1");
        assertThat(instance.getApiServerPort()).isEqualTo(6443);
        assertThat(instance.getCurrentNamespace()).isEqualTo("default");
        assertThat(instance.getApiTimeout()).isPositive();
        assertThat(instance.getWebsocketTimeout()).isPositive();
        assertThat(instance.getClientCertData()).isNotBlank();
        assertThat(instance.getClientKeyData()).isNotBlank();
        assertThat(instance.getApiVersion()).isEqualTo("v1");
        assertThat(instance.verifyHost()).isTrue();
        assertThat(instance.useSSL()).isTrue();

        // assert config overrides
        instance.setCurrentNamespace("foo");
        instance.setApiTimeout(1555);
        instance.setWebsocketTimeout(2555);
        assertThat(instance.getCurrentNamespace()).isEqualTo("foo");
        assertThat(instance.getApiTimeout()).isEqualTo(1555);
        assertThat(instance.getWebsocketTimeout()).isEqualTo(2555);
    }

    @Test
    void should_build_config_with_tls_info_as_files() {
        KubernetesConfig instance = KubernetesConfig.newInstance("src/test/resources/config_pem_files.yaml");
        assertThat(instance.getCaCertData()).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(instance.getClientCertData()).startsWith("-----BEGIN CERTIFICATE-----").isNotEqualTo(instance.getCaCertData());
        assertThat(instance.getClientKeyData()).startsWith("-----BEGIN PRIVATE KEY-----");
    }

    @Test
    void should_build_config_with_file_with_explicit_namespace() {
        KubernetesConfig instance = KubernetesConfig.newInstance("src/test/resources/config_ns.yaml");
        assertThat(instance.getCurrentNamespace()).isEqualTo("my-test-ns");
        instance.setCurrentNamespace("foo");
        assertThat(instance.getCurrentNamespace()).isEqualTo("foo");
    }

    @Test
    void should_build_load_token_from_file() {
        KubernetesConfig instance = KubernetesConfig.newInstance();
        assertThat(instance.getAccessToken()).isNull();
        instance.loadServiceAccountToken("src/test/resources/token");
        assertThat(instance.getAccessToken()).isNotBlank();
    }
}
