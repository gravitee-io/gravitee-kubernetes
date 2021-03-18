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
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.managers.GatewayResourceManager;
import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.services.listeners.PluginsResourceListener;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GatewayResourceWatcher
    extends AbstractResourceWatcher<GatewayResource, WatchActionContext<GatewayResource>>
    implements PluginsResourceListener {

    private final GatewayResourceService gatewayResourceService;

    private final GatewayResourceManager gatewayManager;

    public GatewayResourceWatcher(
        WorkQueueManager<GatewayResource, WatchActionContext<GatewayResource>> workQueue,
        GatewayResourceManager gatewayManager,
        PluginsResourceService pluginsResourceService,
        GatewayResourceService gatewayResourceService
    ) {
        super(workQueue);
        this.gatewayManager = gatewayManager;
        this.gatewayResourceService = gatewayResourceService;
        pluginsResourceService.registerListener(this);
    }

    @Override
    public void onClose(KubernetesClientException e) {
        if (e != null) {
            logger.debug("Exception received and close Gateway watcher, create a new instance of watcher", e);
            this.gatewayManager.initializeWatcher();
        }
    }

    @Override
    public void onPluginsUpdate(WatchActionContext<PluginResource> context) {
        List<GatewayResource> services = gatewayResourceService.listAllGateways();
        for (GatewayResource resource : services) {
            WatchActionContext<GatewayResource> derivedContext = new WatchActionContext<>(
                resource,
                WatchActionContext.Event.REFERENCE_UPDATED
            );
            workQueue.emit(derivedContext);
        }
    }

    @Override
    protected WatchActionContext<GatewayResource> createWatchActionContext(GatewayResource resource, WatchActionContext.Event event) {
        return new WatchActionContext<>(resource, event);
    }
}
