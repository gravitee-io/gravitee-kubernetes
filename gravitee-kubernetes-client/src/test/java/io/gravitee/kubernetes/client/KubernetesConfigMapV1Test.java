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
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@RunWith(VertxUnitRunner.class)
public class KubernetesConfigMapV1Test extends KubernetesUnitTest {

    private ConfigMap configMap1;
    private ConfigMap configMap2;
    private ConfigMap configMap3;

    @Before
    public void before() {
        super.before();
        Map<String, String> configMapData1 = new HashMap<>();
        configMapData1.put("host", "localhost1");
        configMapData1.put("port", "123");

        Map<String, String> configMapData2 = new HashMap<>();
        configMapData2.put("host", "localhost2");
        configMapData2.put("port", "456");

        configMap1 = buildConfigMap(UUID.randomUUID().toString(), "configMap1", configMapData1);
        configMap2 = buildConfigMap(UUID.randomUUID().toString(), "configMap2", configMapData2);
        configMap3 = buildConfigMap(UUID.randomUUID().toString(), "configMap3", Collections.emptyMap());
    }

    @Test
    public void shouldGetConfigMapList(TestContext tc) {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps")
            .andReturn(
                200,
                new ConfigMapListBuilder().addToItems(configMap1, configMap2).withNewMetadata("1", 2L, "1234", "/selflink").build()
            )
            .once();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.ConfigMapList> obs = kubernetesClient
            .get(ResourceQuery.configMaps("test").build())
            .test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            configMapList -> {
                tc.assertEquals(2, configMapList.getItems().size());
                return true;
            }
        );
    }

    @Test
    public void shouldGetConfigMap1(TestContext tc) {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps/configmap1")
            .andReturn(200, new ConfigMapBuilder(configMap1).build())
            .once();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.ConfigMap> obs = kubernetesClient
            .get(ResourceQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps/configmap1").build())
            .test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            configMap -> {
                tc.assertNotNull(configMap.getData());
                tc.assertEquals(2, configMap.getData().size());
                tc.assertNotNull(configMap.getData().get("host"));
                tc.assertNotNull(configMap.getData().get("port"));
                return true;
            }
        );
    }

    @Test
    public void shouldRetrieveSingleKeyInConfigMap(TestContext tc) {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps/configmap1")
            .andReturn(200, new ConfigMapBuilder(configMap1).build())
            .once();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.ConfigMap> obs = kubernetesClient
            .get(ResourceQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps/configmap1/host").build())
            .test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            configMap -> {
                tc.assertNotNull(configMap);
                tc.assertEquals("localhost1", configMap.getData().get("host"));
                return true;
            }
        );
    }

    @Test
    public void shouldWatchAllConfigMaps() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "ADDED"))
            .immediately()
            .andEmit(new WatchEvent(configMap2, "DELETED"))
            .immediately()
            .andEmit(new WatchEvent(configMap3, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.ConfigMap>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps").build())
            .test();

        obs.awaitTerminalEvent();
        obs.assertValueCount(4);
        obs.assertComplete();
    }

    @Test
    public void shouldWatchSpecifiedConfigMap() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?fieldSelector=metadata.name%3DconfigMap1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "DELETED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.ConfigMap>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps/configMap1").build())
            .test();

        obs.awaitTerminalEvent();
        obs.assertValueAt(0, configMapEvent -> configMapEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, configMapEvent -> configMapEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertComplete();
    }

    @Test
    public void shouldWatchConfigMapsWithError() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?fieldSelector=metadata.name%3DconfigMap1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "ERROR"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.ConfigMap>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps/configMap1").build())
            .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
            .test();

        obs.awaitTerminalEvent();
        obs.assertValueAt(0, configMapEvent -> configMapEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertError(Exception.class);
    }

    @Test
    @Ignore
    public void shouldRetryWatchOnEventError() {
        // Mock first connection.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?fieldSelector=metadata.name%3DconfigMap1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "ERROR"))
            .done()
            .once();

        // Mock second connection for retry.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/configmaps?fieldSelector=metadata.name%3DconfigMap1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(configMap1, "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.ConfigMap>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps/configMap1").build())
            .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
            .retry(2)
            .test();

        obs.awaitTerminalEvent();
        obs.assertValueAt(0, configMapEvent -> configMapEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, configMapEvent -> configMapEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertComplete();
    }

    @Test
    public void shouldRetryWatchOnConnectionError() {
        // Shutdown the server to force reconnection.
        server.shutdown();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.ConfigMap>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.ConfigMap>from("/test/configmaps/configMap1").build())
            .retryWhen(
                errors -> {
                    AtomicInteger counter = new AtomicInteger(0);
                    return errors.flatMapSingle(
                        e -> {
                            if (counter.incrementAndGet() >= 5) {
                                return Single.error(e);
                            } else {
                                return Single.timer(50, TimeUnit.MILLISECONDS);
                            }
                        }
                    );
                }
            )
            .test();

        obs.awaitTerminalEvent();
        obs.assertError(Exception.class);
    }

    protected ConfigMap buildConfigMap(String uid, String name, Map<String, String> data) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setNamespace("test");
        metadata.setName(name);
        metadata.setUid(uid);

        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata(metadata);
        configMap.setData(data);

        return configMap;
    }
}
