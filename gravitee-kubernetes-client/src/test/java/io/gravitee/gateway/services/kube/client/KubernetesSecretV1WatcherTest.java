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

import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.services.kube.client.model.v1.Event;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@RunWith(VertxUnitRunner.class)
public class KubernetesSecretV1WatcherTest extends KubernetestUnitTest {

    private KubernetesSecretV1Watcher secretV1Watcher;

    @Test
    public void watchAllResources() {
        secretV1Watcher = new KubernetesSecretV1Watcher(vertx, kubernetesClient, kubernetesConfig);
        List<Event> events = new ArrayList<>();
        try {
            CountDownLatch latch = new CountDownLatch(5);
            secretV1Watcher
                .watch("test")
                .doOnNext(
                    event -> {
                        events.add(event);
                        latch.countDown();
                    }
                )
                .doOnError(Throwable::printStackTrace)
                .subscribe();

            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        assertEquals(5, events.size());
    }

    @Test
    public void watchSpecificResource() {
        secretV1Watcher = new KubernetesSecretV1Watcher(vertx, kubernetesClient, kubernetesConfig);
        List<Event> events = new ArrayList<>();
        try {
            CountDownLatch latch = new CountDownLatch(2);
            secretV1Watcher
                .watch("test", "metadata.name=configmap1")
                .doOnNext(
                    event -> {
                        events.add(event);
                        latch.countDown();
                    }
                )
                .doOnError(Throwable::printStackTrace)
                .subscribe();

            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        assertEquals(2, events.size());
    }
}
