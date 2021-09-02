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
package io.gravitee.gateway.services.kube.client;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.gravitee.gateway.services.kube.client.config.KubernetesConfig;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@RunWith(VertxUnitRunner.class)
public class KubernetesClientV1ImplTest {

    protected Vertx vertx = Vertx.vertx();
    protected KubernetesMockServer server;
    protected KubernetesClient kubernetesClient;

    @Before
    public void setup(TestContext tc) {
        vertx.exceptionHandler(tc.exceptionHandler());

        Map<String, String> configMapData = new HashMap<>();
        configMapData.put("host", "localhost");
        configMapData.put("port", "123");

        ConfigMap configMap1 = getConfigMap("test", UUID.randomUUID().toString(), "configmap1", configMapData);
        ConfigMap configMap2 = getConfigMap("test", UUID.randomUUID().toString(), "configmap2", configMapData);

        Map<String, String> secretData = new HashMap<>();
        secretData.put("tls.key", "dHNsLmtleQ==");
        secretData.put("tls.pem", "dHNsLnBlbQ==");
        Secret secret1 = getSecret("test", UUID.randomUUID().toString(), "secret1", secretData);
        Secret secret2 = getSecret("test", UUID.randomUUID().toString(), "secret2", secretData);

        server = new KubernetesMockServer(true);

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets")
            .andReturn(200, new SecretListBuilder().addToItems(secret1, secret2).withNewMetadata("1", 2L, "1234", "/selflink").build())
            .always();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1")
            .andReturn(200, new SecretBuilder(secret1).build())
            .always();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps")
            .andReturn(
                200,
                new ConfigMapListBuilder().addToItems(configMap1, configMap2).withNewMetadata("1", 2L, "1234", "/selflink").build()
            )
            .always();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps/configmap1")
            .andReturn(200, new ConfigMapBuilder(configMap1).build())
            .always();

        server.init();

        kubernetesClient = new KubernetesClientV1Impl(vertx, config(server.createClient()));
    }

    @After
    public void tearDown() {
        vertx.exceptionHandler(null);
        server.destroy();
        vertx.close();
    }

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
            .secret("test", "secret1")
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
            .configMap("test", "configmap1")
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

    // Helper methods
    private KubernetesConfig config(NamespacedKubernetesClient client) {
        KubernetesConfig config = new KubernetesConfig();
        config.setApiServerHost(client.getMasterUrl().getHost());
        config.setApiServerPort(client.getMasterUrl().getPort());
        config.setCaCertData(
            new String(Base64.getDecoder().decode(client.getConfiguration().getCaCertData().getBytes(StandardCharsets.UTF_8)))
        );
        config.setServiceAccountToken(client.getConfiguration().getOauthToken());
        config.setVerifyHost(false);

        return config;
    }

    private ConfigMap getConfigMap(String namespace, String uid, String name, Map<String, String> data) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setNamespace(namespace);
        metadata.setName(name);
        metadata.setUid(uid);

        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata(metadata);
        configMap.setData(data);

        return configMap;
    }

    private Secret getSecret(String namespace, String uid, String name, Map<String, String> data) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setNamespace(namespace);
        metadata.setName(name);
        metadata.setUid(uid);

        Secret secret = new Secret();
        secret.setMetadata(metadata);
        secret.setData(data);

        return secret;
    }
}
