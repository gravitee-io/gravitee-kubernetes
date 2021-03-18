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

import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResourceList;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.watcher.PluginResourceWatcher;
import io.gravitee.gateway.services.kube.workqueue.WorkQueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PluginsResourceManager extends AbstractResourceManager<PluginResource, WatchActionContext<PluginResource>> {

    @Autowired
    private PluginsResourceService pluginsResourceService;

    @Override
    protected void initializeFlowProcessor() {
        this.workQueueManager = new WorkQueueManager<>(this::process);
    }

    private void process(WatchActionContext<PluginResource> incomingContext) {
        pluginsResourceService
            .processAction(incomingContext)
            .blockingSubscribe(
                context -> LOGGER.debug("Context integration successful"),
                error -> {
                    if (error instanceof PipelineException) {
                        final WatchActionContext<PluginResource> context = ((PipelineException) error).getContext();
                        LOGGER.error(
                            "Process Action on PluginResource fails on resource '{}' : {}",
                            context.getResourceName(),
                            error.getMessage()
                        );
                        pluginsResourceService.persistAsError(context, error.getMessage());
                    } else {
                        LOGGER.error("Process Action on PluginResource fails with unexpected error", error);
                    }
                }
            );
    }

    @Override
    protected void reloadExistingResources() {
        LOGGER.info("Plugins resources loading...");
        PluginResourceList plugins = this.pluginsResourceService.getCrdClient().list();
        if (plugins != null) {
            plugins
                .getItems()
                .forEach(
                    plugin -> {
                        WatchActionContext<PluginResource> action = new WatchActionContext<>(plugin, WatchActionContext.Event.ADDED);
                        action.setReloading(true);
                        this.workQueueManager.emit(action);
                    }
                );
        }
        LOGGER.info("Plugins resources loaded!");
    }

    @Override
    public void initializeWatcher() {
        if (this.lifecycle.moveToStarted() || this.lifecycle.started()) {
            this.watcher = this.pluginsResourceService.getCrdClient().watch(new PluginResourceWatcher(this.workQueueManager, this));
        }
    }
}
