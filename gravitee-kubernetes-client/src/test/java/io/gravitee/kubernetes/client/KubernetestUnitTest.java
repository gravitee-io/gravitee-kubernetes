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

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.vertx.ext.unit.TestContext;
import io.vertx.reactivex.core.Vertx;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetestUnitTest {

    private static final Long EVENT_WAIT_PERIOD_MS = 10L;
    protected Vertx vertx = Vertx.vertx();
    protected KubernetesMockServer server;
    protected KubernetesClient kubernetesClient;
    protected KubernetesConfig kubernetesConfig;

    @Before
    public void setup(TestContext tc) {
        vertx.exceptionHandler(tc.exceptionHandler());

        Map<String, String> configMapData = new HashMap<>();
        configMapData.put("host", "localhost");
        configMapData.put("port", "123");

        ConfigMap configMap1 = getConfigMap("test", UUID.randomUUID().toString(), "configmap1", configMapData);
        ConfigMap configMap2 = getConfigMap("test", UUID.randomUUID().toString(), "configmap2", configMapData);
        ConfigMap configMap3 = getConfigMap("test", UUID.randomUUID().toString(), "configmap3", configMapData);
        ConfigMap configMap4 = getConfigMap("test", UUID.randomUUID().toString(), "configmap4", configMapData);

        Map<String, String> secretData = new HashMap<>();
        secretData.put("tls.key", "dHNsLmtleQ==");
        secretData.put("tls.pem", "dHNsLnBlbQ==");
        Secret secret1 = getSecret("test", UUID.randomUUID().toString(), "secret1", secretData);
        Secret secret2 = getSecret("test", UUID.randomUUID().toString(), "secret2", secretData);
        Secret secret3 = getSecret("test", UUID.randomUUID().toString(), "secret3", secretData);
        Secret secret4 = getSecret("test", UUID.randomUUID().toString(), "secret4", secretData);

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
            .withPath("/api/v1/namespaces/test/secrets/secret2")
            .andReturn(200, new SecretBuilder(secret2).build())
            .always();
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret3")
            .andReturn(200, new SecretBuilder(secret3).build())
            .always();
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret4")
            .andReturn(200, new SecretBuilder(secret4).build())
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

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps/configmap2")
            .andReturn(200, new ConfigMapBuilder(configMap2).build())
            .always();
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps/configmap3")
            .andReturn(200, new ConfigMapBuilder(configMap3).build())
            .always();
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps/configmap4")
            .andReturn(200, new ConfigMapBuilder(configMap4).build())
            .always();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?watch=true&allowWatchBookmarks=true&fieldSelector=")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap3, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap4, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "DELETED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(addFakeData(configMap3), "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(addFakeData(configMap4), "MODIFIED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=configmap1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(addFakeData(configMap1), "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "DELETED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret3, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret4, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(addFakeData(secret3), "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(addFakeData(secret4), "MODIFIED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(addFakeData(secret2), "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        server.init();

        kubernetesConfig = config(server.createClient());
        kubernetesClient = new KubernetesClientV1Impl(vertx, kubernetesConfig);
    }

    @After
    public void tearDown() {
        vertx.exceptionHandler(null);
        server.destroy();
        vertx.close();
    }

    // Helper methods
    protected KubernetesConfig config(NamespacedKubernetesClient client) {
        KubernetesConfig config = new KubernetesConfig();
        config.setApiServerHost(client.getMasterUrl().getHost());
        config.setApiServerPort(client.getMasterUrl().getPort());
        config.setCaCertData(
            new String(Base64.getDecoder().decode(client.getConfiguration().getCaCertData().getBytes(StandardCharsets.UTF_8)))
        );
        config.setAccessToken(client.getConfiguration().getOauthToken());
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

    private KubernetesResource addFakeData(ConfigMap configMap) {
        configMap.getData().put("uuid", UUID.randomUUID().toString());

        return configMap;
    }

    private KubernetesResource addFakeData(Secret secret) {
        secret.getData().put("uuid", UUID.randomUUID().toString());

        return secret;
    }
}
