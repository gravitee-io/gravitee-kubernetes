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
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.managers.ServicesResourceManager;
import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.ServicesResourceService;
import io.gravitee.gateway.services.kube.services.impl.ServiceWatchActionContext;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext.Event;
import io.gravitee.gateway.services.kube.services.listeners.GatewayResourceListener;
import io.gravitee.gateway.services.kube.services.listeners.PluginsResourceListener;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceResourceWatcher
    extends AbstractResourceWatcher<ServicesResource, ServiceWatchActionContext>
    implements PluginsResourceListener, GatewayResourceListener {

    private final ServicesResourceService servicesService;

    private final ServicesResourceManager servicesManager;

    public ServiceResourceWatcher(
        WorkQueueManager<ServicesResource, ServiceWatchActionContext> workQueueManager,
        ServicesResourceManager servicesManager,
        PluginsResourceService pluginsService,
        GatewayResourceService gatewayService,
        ServicesResourceService servicesService
    ) {
        super(workQueueManager);
        this.servicesManager = servicesManager;
        this.servicesService = servicesService;
        gatewayService.registerListener(this);
        pluginsService.registerListener(this);
    }

    @Override
    public void onClose(KubernetesClientException e) {
        if (e != null) {
            logger.debug("Exception received and close Service watcher, create a new instance of watcher", e);
            this.servicesManager.initializeWatcher();
        }
    }

    @Override
    public void onGatewayUpdate(WatchActionContext<GatewayResource> context) {
        triggerServiceUpdates(context);
    }

    @Override
    public void onPluginsUpdate(WatchActionContext<PluginResource> context) {
        triggerServiceUpdates(context);
    }

    private void triggerServiceUpdates(WatchActionContext<? extends CustomResource> context) {
        List<ServicesResource> services = servicesService.listAllServices();
        for (ServicesResource resource : services) {
            ServiceWatchActionContext derivedContext = new ServiceWatchActionContext(resource, Event.REFERENCE_UPDATED);
            workQueue.emit(derivedContext);
        }
    }

    @Override
    protected ServiceWatchActionContext createWatchActionContext(ServicesResource resource, Event event) {
        return new ServiceWatchActionContext(resource, event);
    }
}
