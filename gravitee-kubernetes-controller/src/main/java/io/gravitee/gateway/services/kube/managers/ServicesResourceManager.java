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

import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResourceList;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.ServicesResourceService;
import io.gravitee.gateway.services.kube.services.impl.ServiceWatchActionContext;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.watcher.ServiceResourceWatcher;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ServicesResourceManager extends AbstractResourceManager<ServicesResource, ServiceWatchActionContext> {

    @Autowired
    private ServicesResourceService graviteeServices;

    @Autowired
    private PluginsResourceService pluginsService;

    @Autowired
    private GatewayResourceService gatewayService;

    @Override
    protected void initializeFlowProcessor() {
        this.workQueueManager = new WorkQueueManager<>(this::process);
    }

    protected void process(ServiceWatchActionContext incomingContext) {
        graviteeServices
            .processAction(incomingContext)
            .blockingSubscribe(
                context -> LOGGER.debug("Context integration successful"),
                error -> {
                    if (error instanceof PipelineException) {
                        final WatchActionContext<ServicesResource> context = ((PipelineException) error).getContext();
                        LOGGER.error(
                            "Process Action on ServicesResource fails on resource '{}' : {}",
                            ((PipelineException) error).getContext().getResource(),
                            error.getMessage()
                        );
                        graviteeServices.persistAsError(context, error.getMessage());
                    } else {
                        LOGGER.error("Process Action on ServicesResource fails with unexpected error", error);
                    }
                }
            );
    }

    @Override
    protected void reloadExistingResources() {
        LOGGER.info("Services resources loading...");
        ServicesResourceList services = this.graviteeServices.getCrdClient().list();
        if (services != null) {
            services
                .getItems()
                .forEach(
                    service -> {
                        ServiceWatchActionContext action = new ServiceWatchActionContext(service, WatchActionContext.Event.ADDED);
                        action.setReloading(true);
                        this.workQueueManager.emit(action);
                    }
                );
        }
        LOGGER.info("Services resources loaded!");
    }

    @Override
    public void initializeWatcher() {
        this.watcher =
            this.graviteeServices.getCrdClient()
                .watch(new ServiceResourceWatcher(workQueueManager, this, pluginsService, gatewayService, graviteeServices));
    }
}
