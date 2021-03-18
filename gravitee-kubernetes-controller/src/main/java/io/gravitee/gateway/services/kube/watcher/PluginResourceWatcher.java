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

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.managers.PluginsResourceManager;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PluginResourceWatcher extends AbstractResourceWatcher<PluginResource, WatchActionContext<PluginResource>> {

    private final PluginsResourceManager pluginsManager;

    public PluginResourceWatcher(
        WorkQueueManager<PluginResource, WatchActionContext<PluginResource>> workQueue,
        PluginsResourceManager manager
    ) {
        super(workQueue);
        this.pluginsManager = manager;
    }

    @Override
    public void onClose(KubernetesClientException e) {
        if (e != null) {
            logger.debug("Exception received and close plugin watcher, create a new instance of watcher", e);
            this.pluginsManager.initializeWatcher();
        }
    }

    @Override
    protected WatchActionContext<PluginResource> createWatchActionContext(PluginResource resource, WatchActionContext.Event event) {
        return new WatchActionContext<>(resource, event);
    }
}
