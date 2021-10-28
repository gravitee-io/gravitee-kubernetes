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
package io.gravitee.kubernetes.client;

import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@RunWith(VertxUnitRunner.class)
public class KubernetesClientV1ImplTest extends KubernetestUnitTest {

    @Test
    public void testSecretList(TestContext tc) {
        Async async = tc.async();
        kubernetesClient
            .secretList("test")
            .doOnSuccess(
                secretList -> {
                    tc.assertEquals(2, secretList.getItems().size());
                    async.complete();
                }
            )
            .doOnError(tc::fail)
            .subscribe();
    }

    @Test
    public void testSecret(TestContext tc) {
        Async async = tc.async();
        kubernetesClient
            .get("/test/secrets/secret1", Secret.class)
            .doOnSuccess(
                secret -> {
                    tc.assertNotNull(secret.getData());
                    tc.assertEquals(2, secret.getData().size());
                    tc.assertNotNull(secret.getData().get("tls.key"));
                    tc.assertNotNull(secret.getData().get("tls.pem"));
                    async.complete();
                }
            )
            .doOnError(tc::fail)
            .subscribe();
    }

    @Test
    public void retrieveSingleKeyInSecret(TestContext tc) {
        Async async = tc.async();
        kubernetesClient
            .get("/test/secrets/secret1/tls.key", String.class)
            .doOnSuccess(
                tlsKey -> {
                    tc.assertNotNull(tlsKey);
                    tc.assertEquals("dHNsLmtleQ==", tlsKey);
                    async.complete();
                }
            )
            .doOnError(tc::fail)
            .subscribe();
    }

    @Test
    public void testConfigMapList(TestContext tc) {
        Async async = tc.async();
        kubernetesClient
            .configMapList("test")
            .doOnSuccess(
                configMapList -> {
                    tc.assertEquals(2, configMapList.getItems().size());
                    async.complete();
                }
            )
            .doOnError(tc::fail)
            .subscribe();
    }

    @Test
    public void testConfigMap(TestContext tc) {
        Async async = tc.async();
        kubernetesClient
            .get("/test/configmaps/configmap1", ConfigMap.class)
            .doOnSuccess(
                configMap -> {
                    tc.assertNotNull(configMap.getData());
                    tc.assertEquals(2, configMap.getData().size());
                    tc.assertNotNull(configMap.getData().get("host"));
                    tc.assertNotNull(configMap.getData().get("port"));
                    async.complete();
                }
            )
            .doOnError(tc::fail)
            .subscribe();
    }

    @Test
    public void retrieveSingleKeyInConfigMap(TestContext tc) {
        Async async = tc.async();
        kubernetesClient
            .get("/test/configmaps/configmap1/host", String.class)
            .doOnSuccess(
                host -> {
                    tc.assertNotNull(host);
                    tc.assertEquals("localhost", host);
                    async.complete();
                }
            )
            .doOnError(tc::fail)
            .subscribe();
    }
}
