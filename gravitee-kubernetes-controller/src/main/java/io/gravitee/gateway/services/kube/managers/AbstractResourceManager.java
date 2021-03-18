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
package io.gravitee.gateway.services.kube.managers;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.Watch;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractResourceManager<CR extends CustomResource, T extends WatchActionContext<CR>>
    extends AbstractLifecycleComponent<AbstractResourceManager<CR, T>> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected Watch watcher;

    protected WorkQueueManager<CR, T> workQueueManager;

    @Override
    protected void doStart() throws Exception {
        if (!this.lifecycle.started()) {
            initializeFlowProcessor();
            reloadExistingResources();
            initializeWatcher();
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (this.watcher != null) {
            LOGGER.info("Close services watcher");
            this.watcher.close();
        }

        if (this.workQueueManager != null) {
            this.workQueueManager.shutdown();
        }
    }

    protected abstract void initializeFlowProcessor();

    protected abstract void reloadExistingResources();

    public abstract void initializeWatcher();
}
