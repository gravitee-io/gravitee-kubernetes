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

import io.fabric8.kubernetes.api.model.*;
import io.gravitee.kubernetes.client.api.FieldSelector;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class KubernetesEndpointsV1Test extends KubernetesUnitTest {

    @Test
    public void should_get_endpoints_list(TestContext tc) throws InterruptedException {
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints").andReturn(200, buildEndpoints()).always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.EndpointsList> obs = kubernetesClient
            .get(ResourceQuery.endpoints("test").build())
            .test();

        obs.await();
        obs.assertValue(endpointsList -> {
            tc.assertEquals(1, endpointsList.getItems().size());
            return true;
        });
    }

    @Test
    public void should_get_nothing_on_error() throws InterruptedException {
        server.expect().get().withPath("/api/v1/namespaces/test/endpoints/unknown").andReturn(404, "not found").always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.Endpoints> obs = kubernetesClient
            .get(ResourceQuery.<io.gravitee.kubernetes.client.model.v1.Endpoints>from("/test/endpoints/unknown").build())
            .test();

        obs.await();
        obs.assertError(RuntimeException.class);
    }

    @Test
    public void should_watch_endpoints_list() throws InterruptedException {
        final Endpoints endpoints = buildEndpoints().getItems().get(0);

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/endpoints?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(endpoints, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/endpoints").build()).test();

        obs.await();
        obs.assertValueCount(4);
        obs.assertValueAt(0, endpointsListEvent -> endpointsListEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, endpointsListEvent -> endpointsListEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(2, endpointsListEvent -> endpointsListEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertValueAt(3, endpointsListEvent -> endpointsListEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_repeat_on_disconnect() throws InterruptedException {
        final Endpoints endpoints = buildEndpoints().getItems().get(0);

        var path = "/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3DtestEndpoints&watch=true";

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
            .andEmit(new WatchEvent(endpoints, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "MODIFIED"))
            .done()
            .once();

        var obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Endpoints>from("/test/endpoints/testEndpoints").build())
            .test();

        obs.await();
        obs.assertValueCount(2);
        obs.assertValueAt(0, endpointsEvent -> endpointsEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, endpointsEvent -> endpointsEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_watch_endpoints_with_label_and_field_selectors() throws InterruptedException {
        final Endpoints endpoints = buildEndpoints().getItems().get(0);

        server
            .expect()
            .get()
            .withPath(
                "/api/v1/namespaces/test/endpoints?fieldSelector=field1%3DvalueField1,field2%3DvalueField2&labelSelector=label1%3DvalueLabel1,label2%3DvalueLabel2&watch=true"
            )
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(endpoints, "MODIFIED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "MODIFIED"))
            .done()
            .once();

        final Flowable<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> watch1 =
            kubernetesClient.watch(
                WatchQuery
                    .<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/endpoints")
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
    public void should_complete_watch_endpoints_after_error() throws InterruptedException {
        final Endpoints endpoints = buildEndpoints().getItems().get(0);

        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3DtestEndpoints&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(endpoints, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "ERROR"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient
                .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/endpoints/testEndpoints").build())
                .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
                .test();

        obs.await();
        obs.assertValueAt(0, endpointsEvent -> endpointsEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertError(Exception.class);
    }

    @Test
    public void should_retry_watch_on_event_error() throws InterruptedException {
        final Endpoints endpoints = buildEndpoints().getItems().get(0);

        // Mock first connection.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3DtestEndpoints&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(endpoints, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "ERROR"))
            .done()
            .once();

        // Mock second connection for retry.
        server
            .expect()
            .get()
            .withPath("/api/v1/namespaces/test/endpoints?fieldSelector=metadata.name%3DtestEndpoints&watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(endpoints), "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient
                .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/endpoints/testEndpoints").build())
                .flatMapSingle(e -> !e.getType().equalsIgnoreCase("ERROR") ? Single.just(e) : Single.error(new Exception("fake error")))
                .retry(2)
                .test();

        obs.await();
        obs.assertValueAt(0, endpointsEvent -> endpointsEvent.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, endpointsEvent -> endpointsEvent.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_retry_watch_on_connection_error() throws InterruptedException {
        // Shutdown the server to force reconnection.
        server.shutdown();

        final TestSubscriber<io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs =
            kubernetesClient
                .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/endpoints").build())
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

    private Endpoints incrementResourceVersion(Endpoints endpoints) {
        int i = Integer.parseInt(endpoints.getMetadata().getResourceVersion());
        endpoints.getMetadata().setResourceVersion(String.valueOf(i + 1));
        return endpoints;
    }

    private static @NonNull EndpointsList buildEndpoints() {
        final Endpoints endpoints = new Endpoints();
        ArrayList<EndpointSubset> subsets = new ArrayList<>();

        endpoints.setSubsets(subsets);
        endpoints.setMetadata(new ObjectMetaBuilder().withName("testEndpoints").withResourceVersion("1234").build());

        EndpointSubset endpointSubset = new EndpointSubset();
        subsets.add(endpointSubset);
        ArrayList<EndpointAddress> addresses = new ArrayList<>();
        endpointSubset.setAddresses(addresses);

        for (int i = 0; i < 5; i++) {
            EndpointAddress endpointAddress = new EndpointAddress();
            addresses.add(endpointAddress);

            ObjectReference targetRef = new ObjectReference();
            targetRef.setUid("" + i);
            endpointAddress.setTargetRef(targetRef);
        }

        return new EndpointsListBuilder().addToItems(endpoints).withNewMetadata("1", 0L, "1234", "/selflink").build();
    }
}
