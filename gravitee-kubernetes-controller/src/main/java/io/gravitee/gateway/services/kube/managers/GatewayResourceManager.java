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

import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResourceList;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.watcher.GatewayResourceWatcher;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GatewayResourceManager extends AbstractResourceManager<GatewayResource, WatchActionContext<GatewayResource>> {

    @Autowired
    private GatewayResourceService gatewayResourceService;

    @Autowired
    private PluginsResourceService pluginsResourceService;

    @Override
    protected void initializeFlowProcessor() {
        this.workQueueManager = new WorkQueueManager<>(this::process);
    }

    private void process(WatchActionContext<GatewayResource> incomingContext) {
        gatewayResourceService
            .processAction(incomingContext)
            .blockingSubscribe(
                context -> LOGGER.debug("Context integration successful"),
                error -> {
                    if (error instanceof PipelineException) {
                        final WatchActionContext<GatewayResource> context = ((PipelineException) error).getContext();
                        LOGGER.error(
                            "Process Action on GatewayResource fails on resource '{}' : {}",
                            context.getResourceName(),
                            error.getMessage()
                        );
                        gatewayResourceService.persistAsError(context, error.getMessage());
                    } else {
                        LOGGER.error("Process Action on GatewayResource fails with unexpected error", error);
                    }
                }
            );
    }

    @Override
    protected void reloadExistingResources() {
        LOGGER.info("Gateway resources loading...");
        GatewayResourceList gateways = this.gatewayResourceService.getCrdClient().list();
        if (gateways != null) {
            gateways
                .getItems()
                .forEach(
                    gateway -> {
                        WatchActionContext<GatewayResource> action = new WatchActionContext<>(gateway, WatchActionContext.Event.ADDED);
                        action.setReloading(true);
                        this.workQueueManager.emit(action);
                    }
                );
        }
        LOGGER.info("Gateway resources loaded!");
    }

    @Override
    public void initializeWatcher() {
        this.watcher =
            this.gatewayResourceService.getCrdClient()
                .watch(new GatewayResourceWatcher(workQueueManager, this, pluginsResourceService, gatewayResourceService));
    }
}
