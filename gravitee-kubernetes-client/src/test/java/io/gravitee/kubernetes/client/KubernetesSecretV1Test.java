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

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.gravitee.kubernetes.client.api.FieldSelector;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
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
    public void shouldGetSecretList(TestContext tc) throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets")
            .andReturn(200, new SecretListBuilder().addToItems(secret1, secret2).withNewMetadata("1", 2L, "1234", "/selflink").build())
            .always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.SecretList> obs = kubernetesClient
            .get(ResourceQuery.secrets("test").build())
            .test();

        obs.await();
        obs.assertValue(secretList -> {
            tc.assertEquals(2, secretList.getItems().size());
            return true;
        });
    }

    @Test
    public void shouldGetSecret1(TestContext tc) throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1")
            .andReturn(200, new SecretBuilder(secret1).build())
            .always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.Secret> obs = kubernetesClient
            .get(ResourceQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .test();

        obs.await();
        obs.assertValue(secret -> {
            tc.assertNotNull(secret.getData());
            tc.assertEquals(2, secret.getData().size());
            tc.assertNotNull(secret.getData().get("tls.key"));
            tc.assertNotNull(secret.getData().get("tls.pem"));
            return true;
        });
    }

    @Test
    public void shouldRetrieveSingleKeyInSecret(TestContext tc) throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1")
            .andReturn(200, new SecretBuilder(secret1).build())
            .always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.Secret> obs = kubernetesClient
            .get(ResourceQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1/tls.key").build())
            .test();

        obs.await();
        obs.assertValue(secret -> {
            tc.assertNotNull(secret);
            tc.assertEquals("dHNsLmtleQ==", new String(secret.getData().get("tls.key")));
            return true;
        });
    }

    @Test
    public void shouldWatchAllSecrets() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true")
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

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets").build())
            .test();

        obs.await();
        obs.assertValueCount(4);
        obs.assertComplete();
    }

    @Test
    public void shouldWatchSpecifiedSecret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertComplete();
    }

    @Test
    public void shouldWatchSpecifiedSecret_usingDsl() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.secret("test", "secret1").build())
            .test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertComplete();
    }

    @Test
    public void shouldWatchOnceOnSameSecret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );
        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch2 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );
        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = Flowable
            .mergeArray(watch1, watch2)
            .test();

        obs.await();
        obs.assertValueCount(4);
        obs.assertComplete();
    }

    @Test
    public void shouldWatchSecretWithLabelAndFieldSelectors() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath(
                "/api/v1/namespaces/test/secrets/secret1?fieldSelector=field1%3DvalueField1,field2%3DvalueField2&labelSelector=label1%3DvalueLabel1,label2%3DvalueLabel2&watch=true"
            )
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret2, "MODIFIED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 = kubernetesClient.watch(
            WatchQuery
                .<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1")
                .fieldSelector(FieldSelector.equals("field1", "valueField1"))
                .fieldSelector(FieldSelector.equals("field2", "valueField2"))
                .labelSelector(LabelSelector.equals("label1", "valueLabel1"))
                .labelSelector(LabelSelector.equals("label2", "valueLabel2"))
                .build()
        );

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = watch1.test();

        obs.await();
        obs.assertValueCount(2);
        obs.assertComplete();
    }

    @Test
    public void shouldCancelWatchOnSameSecret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
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
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "DELETED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .doOnSubscribe(Subscription::cancel);
        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch2 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs1 = watch1.test();
        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs2 = watch2.test();

        obs1.assertEmpty();

        obs2.await();
        obs2.assertValueCount(2);
        obs2.assertComplete();
    }

    @Test
    public void shouldWatchOnDifferentSecrets() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
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
            .withPath("/api/v1/namespaces/test/secrets/secret2?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret2, "ADDED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );
        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch2 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret2").build()
        );
        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = Flowable
            .mergeArray(watch1, watch2)
            .test();

        obs.await();
        obs.assertValueCount(3);
        obs.assertComplete();
    }

    @Test
    public void shouldCompleteWatchSecretsAfterError() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ERROR"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
            .test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertError(Exception.class);
    }

    @Test
    public void shouldRetryWatchOnEventError() throws InterruptedException {
        // Mock first connection.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
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
            .withPath("/api/v1/namespaces/test/secrets/secret1?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
            .retry(2)
            .test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertComplete();
    }

    @Test
    public void shouldRetryWatchOnConnectionError() throws InterruptedException {
        // Shutdown the server to force reconnection.
        server.shutdown();

        Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch = kubernetesClient.watch(
            WatchQuery.secret("test", "secret1").build()
        );

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .retryWhen(errors -> {
                AtomicInteger counter = new AtomicInteger(0);
                return errors.flatMapSingle(e -> {
                    if (counter.incrementAndGet() >= 5) {
                        return Single.error(e);
                    } else {
                        return Single.timer(50, TimeUnit.MILLISECONDS);
                    }
                });
            })
            .test();

        obs.await();
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
