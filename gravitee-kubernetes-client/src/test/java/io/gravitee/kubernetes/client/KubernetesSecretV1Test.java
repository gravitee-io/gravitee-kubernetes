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
import io.gravitee.kubernetes.client.model.v1.SecretEvent;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscription;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@RunWith(VertxUnitRunner.class)
public class KubernetesSecretV1Test extends KubernetesUnitTest {

    private Secret secret1;
    private Secret secret2;
    private Secret secret3;

    @Before
    public void before() {
        super.before();
        Map<String, String> secretData = new HashMap<>();
        secretData.put("tls.key", "dHNsLmtleQ==");
        secretData.put("tls.pem", "dHNsLnBlbQ==");

        secret1 = buildSecret("test", UUID.randomUUID().toString(), "secret1", secretData);
        secret2 = buildSecret("test", UUID.randomUUID().toString(), "secret2", secretData);
        secret3 = buildSecret("test", UUID.randomUUID().toString(), "secret3", secretData);
    }

    @Test
    public void shouldGetSecretList(TestContext tc) {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets")
            .andReturn(200, new SecretListBuilder().addToItems(secret1, secret2).withNewMetadata("1", 2L, "1234", "/selflink").build())
            .always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.SecretList> obs = kubernetesClient.secretList("test").test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            secretList -> {
                tc.assertEquals(2, secretList.getItems().size());
                return true;
            }
        );
    }

    @Test
    public void shouldGetSecret1(TestContext tc) {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1")
            .andReturn(200, new SecretBuilder(secret1).build())
            .always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.Secret> obs = kubernetesClient
            .get("/test/secrets/secret1", io.gravitee.kubernetes.client.model.v1.Secret.class)
            .test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            secret -> {
                tc.assertNotNull(secret.getData());
                tc.assertEquals(2, secret.getData().size());
                tc.assertNotNull(secret.getData().get("tls.key"));
                tc.assertNotNull(secret.getData().get("tls.pem"));
                return true;
            }
        );
    }

    @Test
    public void shouldRetrieveSingleKeyInSecret(TestContext tc) {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1")
            .andReturn(200, new SecretBuilder(secret1).build())
            .always();

        final TestObserver<String> obs = kubernetesClient.get("/test/secrets/secret1/tls.key", String.class).test();

        obs.awaitTerminalEvent();
        obs.assertValue(
            tlsKey -> {
                tc.assertNotNull(tlsKey);
                tc.assertEquals("dHNsLmtleQ==", tlsKey);
                return true;
            }
        );
    }

    @Test
    public void shouldWatchAllSecrets() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ADDED"))
            .immediately()
            .andEmit(new WatchEvent(secret2, "DELETED"))
            .immediately()
            .andEmit(new WatchEvent(secret3, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<SecretEvent> obs = kubernetesClient.watch("/test/secrets", SecretEvent.class).test();

        obs.awaitTerminalEvent();
        obs.assertValueCount(4);
        obs.assertComplete();
    }

    @Test
    public void shouldWatchSpecifiedSecret() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final TestSubscriber<SecretEvent> obs = kubernetesClient.watch("/test/secrets/secret1", SecretEvent.class).test();

        obs.awaitTerminalEvent();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertComplete();
    }

    @Test
    public void shouldWatchOnceOnSameSecret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final Flowable<SecretEvent> watch1 = kubernetesClient.watch("/test/secrets/secret1", SecretEvent.class);
        final Flowable<SecretEvent> watch2 = kubernetesClient.watch("/test/secrets/secret1", SecretEvent.class);
        final TestSubscriber<SecretEvent> obs = Flowable.mergeArray(watch1, watch2).test();

        obs.awaitTerminalEvent();
        obs.assertValueCount(4);
        obs.assertComplete();
    }

    @Test
    public void shouldCancelWatchOnSameSecret() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final Flowable<SecretEvent> watch1 = kubernetesClient
            .watch("/test/secrets/secret1", SecretEvent.class)
            .doOnSubscribe(Subscription::cancel);
        final Flowable<SecretEvent> watch2 = kubernetesClient.watch("/test/secrets/secret1", SecretEvent.class);

        final TestSubscriber<SecretEvent> obs1 = watch1.test();
        final TestSubscriber<SecretEvent> obs2 = watch2.test();
        //obs1.awaitTerminalEvent();
        obs1.assertEmpty();

        obs2.awaitTerminalEvent();
        obs2.assertValueCount(2);
        obs2.assertComplete();
    }

    @Test
    public void shouldWatchOnDifferentSecrets() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret2")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret2, "ADDED"))
            .done()
            .once();

        final Flowable<SecretEvent> watch1 = kubernetesClient.watch("/test/secrets/secret1", SecretEvent.class);
        final Flowable<SecretEvent> watch2 = kubernetesClient.watch("/test/secrets/secret2", SecretEvent.class);
        final TestSubscriber<SecretEvent> obs = Flowable.mergeArray(watch1, watch2).test();

        obs.awaitTerminalEvent();
        obs.assertValueCount(3);
        obs.assertComplete();
    }

    @Test
    public void testWatchSecretsWithError() {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ERROR"))
            .done()
            .once();

        final TestSubscriber<SecretEvent> obs = kubernetesClient
            .watch("/test/secrets/secret1", SecretEvent.class)
            .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
            .test();

        obs.awaitTerminalEvent();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertError(Exception.class);
    }

    @Test
    public void shouldRetryWatchOnEventError() {
        // Mock first connection.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ERROR"))
            .done()
            .once();

        // Mock second connection for retry.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true&allowWatchBookmarks=true&fieldSelector=metadata.name=secret1")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<SecretEvent> obs = kubernetesClient
            .watch("/test/secrets/secret1", SecretEvent.class)
            .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
            .retry()
            .test();

        obs.awaitTerminalEvent();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertComplete();
    }

    @Test
    public void shouldRetryWatchOnConnectionError() {
        // Shutdown the server to force reconnection.
        server.shutdown();

        final TestSubscriber<SecretEvent> obs = kubernetesClient
            .watch("/test/secrets/secret1", SecretEvent.class)
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

    protected Secret buildSecret(String namespace, String uid, String name, Map<String, String> data) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setNamespace(namespace);
        metadata.setName(name);
        metadata.setUid(uid);

        Secret secret = new Secret();
        secret.setMetadata(metadata);
        secret.setData(data);
        secret.setApiVersion("1.0");

        return secret;
    }
}
