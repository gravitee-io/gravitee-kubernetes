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

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.api.model.discovery.EndpointSlice;
import io.fabric8.kubernetes.api.model.discovery.EndpointSliceList;
import io.fabric8.kubernetes.api.model.discovery.EndpointSliceListBuilder;
import io.fabric8.kubernetes.api.model.discovery.EndpointSlicePortBuilder;
import io.fabric8.kubernetes.api.model.discovery.EndpointSliceBuilder;
import io.gravitee.kubernetes.client.api.FieldSelector;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class KubernetesEndpointSliceV1Test extends KubernetesUnitTest {

    @Test
    public void should_get_endpointslice_list(TestContext tc) throws InterruptedException {
        server
            .expect()
            .get()
            .withPath("/apis/discovery.k8s.io/v1/namespaces/test/endpointslices")
            .andReturn(200, buildEndpointSlices())
            .always();

        final TestObserver<io.gravitee.kubernetes.client.model.v1.EndpointSliceList> obs = kubernetesClient
            .get(ResourceQuery.endpointSlices("test").build())
            .test();

        obs.await();
        obs.assertValue(list -> {
            tc.assertEquals(1, list.getItems().size());
            return true;
        });
    }

    @Test
    public void should_watch_endpointslice_list() throws InterruptedException {
        final EndpointSlice slice = buildEndpointSlices().getItems().get(0);

        server
            .expect()
            .get()
            .withPath("/apis/discovery.k8s.io/v1/namespaces/test/endpointslices?watch=true")
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(slice, "ADDED"))
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(incrementResourceVersion(slice), "MODIFIED"))
            .done()
            .once();

        final TestSubscriber<
            io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.EndpointSlice>
        > obs =
            kubernetesClient.watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.EndpointSlice>from("/test/endpointslices").build()).test();

        obs.await();
        obs.assertValueCount(2);
        obs.assertValueAt(0, event -> event.getType().equalsIgnoreCase("ADDED"));
        obs.assertValueAt(1, event -> event.getType().equalsIgnoreCase("MODIFIED"));
        obs.assertNotComplete();
    }

    @Test
    public void should_watch_endpointslice_with_label_and_field_selectors() throws InterruptedException {
        final EndpointSlice slice = buildEndpointSlices().getItems().get(0);

        server
            .expect()
            .get()
            .withPath(
                "/apis/discovery.k8s.io/v1/namespaces/test/endpointslices?fieldSelector=field1%3DvalueField1,field2%3DvalueField2&labelSelector=label1%3DvalueLabel1,label2%3DvalueLabel2&watch=true"
            )
            .andUpgradeToWebSocket()
            .open()
            .waitFor(EVENT_WAIT_PERIOD_MS)
            .andEmit(new WatchEvent(slice, "MODIFIED"))
            .done()
            .once();

        final Flowable<
            io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.EndpointSlice>
        > watch =
            kubernetesClient.watch(
                WatchQuery
                    .<io.gravitee.kubernetes.client.model.v1.EndpointSlice>from("/test/endpointslices")
                    .fieldSelector(FieldSelector.equals("field1", "valueField1"))
                    .fieldSelector(FieldSelector.equals("field2", "valueField2"))
                    .labelSelector(LabelSelector.equals("label1", "valueLabel1"))
                    .labelSelector(LabelSelector.equals("label2", "valueLabel2"))
                    .build()
            );

        final TestSubscriber<
            io.gravitee.kubernetes.client.model.v1.Event<io.gravitee.kubernetes.client.model.v1.EndpointSlice>
        > obs = watch.test();

        obs.await();
        obs.assertValueCount(1);
        obs.assertNotComplete();
    }

    private EndpointSlice incrementResourceVersion(EndpointSlice slice) {
        int i = Integer.parseInt(slice.getMetadata().getResourceVersion());
        slice.getMetadata().setResourceVersion(String.valueOf(i + 1));
        return slice;
    }

    private static EndpointSliceList buildEndpointSlices() {
        EndpointSlice slice = new EndpointSliceBuilder()
            .withMetadata(new ObjectMetaBuilder().withName("testEndpointSlice").withResourceVersion("1234").build())
            .withAddressType("IPv4")
            .withPorts(new EndpointSlicePortBuilder().withPort(8080).build())
            .addNewEndpoint()
            .withAddresses("10.0.0.1")
            .endEndpoint()
            .build();

        return new EndpointSliceListBuilder().addToItems(slice).withNewMetadata("1", 0L, "1234", "/selflink").build();
    }
}
