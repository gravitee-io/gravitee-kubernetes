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
package io.gravitee.gateway.services.kube.workqueue;

import io.fabric8.kubernetes.client.CustomResource;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WorkQueueManager<CR extends CustomResource, T extends WatchActionContext<CR>> {

    private final Queue<T> queue = new LinkedList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final Consumer<T> process;

    public WorkQueueManager(Consumer<T> process) {
        this.process = process;
        executor.schedule(new EventExecutor(), 1, TimeUnit.SECONDS);
    }

    public void emit(T o) {
        queue.offer(o);
    }

    public void shutdown() {
        this.executor.shutdownNow();
    }

    private final class EventExecutor implements Runnable {

        @Override
        public void run() {
            T context;
            do {
                context = queue.poll();
                if (context != null) {
                    process.accept(context);
                }
            } while (context != null);
            executor.schedule(new EventExecutor(), 1, TimeUnit.SECONDS);
        }
    }
}
