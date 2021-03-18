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
package io.gravitee.gateway.services.kube.watcher;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.Watcher;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractResourceWatcher<T extends CustomResource, W extends WatchActionContext<T>> implements Watcher<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final WorkQueueManager<T, W> workQueue;

    protected AbstractResourceWatcher(final WorkQueueManager<T, W> workQueue) {
        this.workQueue = workQueue;
    }

    @Override
    public void eventReceived(Action action, T resource) {
        logger.info("Action {} received for {}", action, resource.getClass().getSimpleName());
        switch (action) {
            case ADDED:
                workQueue.emit(createWatchActionContext(resource, WatchActionContext.Event.ADDED));
                break;
            case MODIFIED:
                workQueue.emit(createWatchActionContext(resource, WatchActionContext.Event.MODIFIED));
                break;
            case DELETED:
                workQueue.emit(createWatchActionContext(resource, WatchActionContext.Event.DELETED));
                break;
            case ERROR:
                logger.warn("Action {} received for {}", action, resource.getClass().getSimpleName());
                break;
            default:
                logger.warn("Unmanaged action {}", action);
        }
    }

    protected abstract W createWatchActionContext(T resource, WatchActionContext.Event event);
}
