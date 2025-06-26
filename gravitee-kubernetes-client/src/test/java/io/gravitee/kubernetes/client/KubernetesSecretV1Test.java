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
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.*;
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
    @Override
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
    public void should_get_secret_list(TestContext tc) throws InterruptedException {
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
    public void should_get_secret1(TestContext tc) throws InterruptedException {
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
    public void should_retrieve_single_key_in_secret(TestContext tc) throws InterruptedException {
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
    public void should_retrieve_nothing_on_error() throws InterruptedException {
        server.expect().get().withPath("/api/v1/namespaces/test/secrets/secret1").andReturn(404, "not found").always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.Secret> obs = kubernetesClient
            .get(ResourceQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1/tls.key").build())
            .test();

        obs.await();
        obs.assertError(RuntimeException.class);
    }

    @Test
    public void should_watch_all_secrets() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret2, "DELETED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret3, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets").build()).test();

        obs.await();
        obs.assertValueCount(4);
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertValueAt(2, secretEvent -> secretEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(3, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertNotComplete();
    }

    @Test
    public void shouldWatchSpecifiedSecret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "DELETED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()).test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_repeat_on_disconnect() throws InterruptedException {
        var path = "/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true";

        // accept watch and disconnect
        server.expect().withPath(path).andUpgradeToWebSocket().open().done().once();

        // accept watch again and emit values
        server
            .expect()
            .get()
            .withPath(path)
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "DELETED"))
            .done()
            .once();

        var obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .test();

        obs.await();
        obs.assertValueCount(2);
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_watch_specified_secret_using_dsl() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "DELETED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient.watch(WatchQuery.secret("test", "secret1").build()).test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("DELETED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_watch_once_on_same_secret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "DELETED"))
            .done()
            .once();

        var watch1 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );
        var watch2 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );

        var obs = Flowable.merge(List.of(watch1, watch2)).test();
        obs.await();
        obs.assertValueCount(4);
        obs.assertNotComplete();
    }

    @Test
    public void should_watch_once_on_same_secret_with_late_subscriber() throws InterruptedException {
        // KubernetesMockServer does not allow for real sequencing triggering of events. All events are sent all in once when calling done() method.
        // For this specific test, we use 2 server mocks to allow for the second subscriber to subscribe later and simulate event loss.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open(new WatchEvent(secret1, "ADDED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "MODIFIED"))
            .done()
            .once();

        var watch1 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );
        var watch2 = kubernetesClient.watch(
            WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build()
        );

        var obs = Flowable.merge(List.of(watch1, watch2.delaySubscription(500, TimeUnit.MILLISECONDS))).test();
        obs.await();
        obs.assertValueCount(4);
        obs.assertNotComplete();
    }

    @Test
    public void should_watch_secret_with_label_and_field_selectors() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath(
                "/api/v1/namespaces/test/secrets?fieldSelector=field1%3DvalueField1,field2%3DvalueField2&labelSelector=label1%3DvalueLabel1,label2%3DvalueLabel2&watch=true"
            )
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret2, "MODIFIED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 =
            kubernetesClient.watch(
                WatchQuery
                    .<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets")
                    .fieldSelector(FieldSelector.equals("field1", "valueField1"))
                    .fieldSelector(FieldSelector.equals("field2", "valueField2"))
                    .labelSelector(LabelSelector.equals("label1", "valueLabel1"))
                    .labelSelector(LabelSelector.equals("label2", "valueLabel2"))
                    .build()
            );

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            watch1.test();

        obs.await();
        obs.assertValueCount(2);
        obs.assertNotComplete();
    }

    @Test
    public void should_cancel_watch_on_same_secret() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "DELETED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 =
            kubernetesClient
                .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
                .doOnSubscribe(Subscription::cancel);

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch2 =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build());

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs1 =
            watch1.test();
        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs2 =
            watch2.test();

        obs1.assertEmpty();

        obs2.await();
        obs2.assertValueCount(2);
        obs2.assertNotComplete();
    }

    @Test
    public void should_watch_on_different_secrets() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "DELETED"))
            .done()
            .once();

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret2&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret2, "ADDED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build());
        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch2 =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret2").build());
        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = Flowable
            .mergeArray(watch1, watch2)
            .test();

        obs.await();
        obs.assertValueCount(3);
        obs.assertNotComplete();
    }

    @Test
    public void should_complete_watch_secrets_after_error() throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ERROR"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient
                .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
                .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
                .test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertError(Exception.class);
    }

    @Test
    public void should_retry_watch_on_event_error() throws InterruptedException {
        // Mock first connection.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(secret1, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "ERROR"))
            .done()
            .once();

        // Mock second connection for retry.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(secret1), "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient
                .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
                .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
                .retry(2)
                .test();

        obs.await();
        obs.assertValueAt(0, secretEvent -> secretEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, secretEvent -> secretEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_retry_watch_on_connection_error() throws InterruptedException {
        // Shutdown the server to force reconnection.
        server.shutdown();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient
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

    @Test
    public void should_create_secret(TestContext tc) throws InterruptedException {
        io.gravitee.kubernetes.client.model.v1.Secret secret = new io.gravitee.kubernetes.client.model.v1.Secret();

        io.gravitee.kubernetes.client.model.v1.ObjectMeta metadata = new io.gravitee.kubernetes.client.model.v1.ObjectMeta();
        metadata.setName("test");
        metadata.setNamespace("test");
        secret.setMetadata(metadata);

        HashMap<String, String> data = new HashMap<>();
        data.put("test", "test");
        secret.setData(data);

        server.expect().post().withPath("/api/v1/namespaces/test/secrets").andReturn(201, secret).once();

        TestObserver<Watchable> obs = kubernetesClient.create(secret).test();
        obs.await(5, TimeUnit.SECONDS);
        obs.assertValue(s -> {
            tc.assertNotNull(s);
            io.gravitee.kubernetes.client.model.v1.Secret value = (io.gravitee.kubernetes.client.model.v1.Secret) s;

            tc.assertEquals("test", value.getMetadata().getName());
            tc.assertEquals("test", value.getMetadata().getNamespace());
            tc.assertNotNull(value.getData());
            tc.assertEquals("test", value.getData().get("test"));

            return true;
        });
    }

    protected Secret buildSecret(String namespace, String uid, String name, Map<String, String> data) {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setNamespace(namespace);
        metadata.setName(name);
        metadata.setUid(uid);
        metadata.setResourceVersion("1");

        Secret secret = new Secret();
        secret.setMetadata(metadata);
        secret.setData(data);
        secret.setApiVersion("1.0");

        return secret;
    }

    private Secret incrementResourceVersion(Secret secret) {
        int i = Integer.parseInt(secret.getMetadata().getResourceVersion());
        secret.getMetadata().setResourceVersion(String.valueOf(i + 1));
        return secret;
    }
}
