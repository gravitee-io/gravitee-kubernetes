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
package io.gravitee.gateway.services.kube;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.services.kube.managers.AbstractResourceManager;
import io.gravitee.gateway.services.kube.managers.GatewayResourceManager;
import io.gravitee.gateway.services.kube.managers.PluginsResourceManager;
import io.gravitee.gateway.services.kube.managers.ServicesResourceManager;
import io.gravitee.gateway.services.kube.utils.Fabric8sMapperUtils;
import io.gravitee.gateway.services.kube.webhook.AdmissionHookManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class KubeSyncService extends AbstractService<KubeSyncService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubeSyncService.class);

    @Value("${services.kubernetes.enabled:false}")
    private boolean enabled;

    @Autowired
    private KubernetesClient client;

    @Autowired
    public GatewayResourceManager gatewayManager;

    @Autowired
    public PluginsResourceManager pluginsManager;

    @Autowired
    public ServicesResourceManager servicesManager;

    @Autowired
    protected AdmissionHookManager hookManager;

    ScheduledExecutorService executorService;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            Fabric8sMapperUtils.initJsonMapper();

            // If the CRDS are not defined when the Gateway start, we try periodically to connect to K8S API Server
            // otherwise GW will not start
            executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.schedule(new StarterThread(executorService), 0, TimeUnit.SECONDS);

            if (hookManager != null) {
                hookManager.registerHooks();
            }
        }
    }

    private void startManagers() throws Exception {
        if (pluginsManager != null) {
            pluginsManager.start();
        }
        if (gatewayManager != null) {
            gatewayManager.start();
        }
        if (servicesManager != null) {
            servicesManager.start();
        }
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.info("Kubernetes Controller stopping...");
        stopManagers();

        if (this.client != null) {
            this.client.close();
        }

        if (this.executorService != null && !this.executorService.isTerminated()) {
            this.executorService.shutdownNow();
        }
    }

    private void stopManagers() {
        stopManagerQuietly(this.servicesManager);
        stopManagerQuietly(this.gatewayManager);
        stopManagerQuietly(this.pluginsManager);
    }

    private void stopManagerQuietly(AbstractResourceManager<?, ?> manager) {
        if (manager != null) {
            try {
                manager.stop();
            } catch (Exception e) {
                LOGGER.warn("'{}' Manager can't be stopped", manager.getClass().getName(), e);
            }
        }
    }

    private final class StarterThread implements Runnable {

        ScheduledExecutorService executorService;

        public StarterThread(ScheduledExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public void run() {
            try {
                LOGGER.debug("Trying to connect to Kubernetes API Server...");
                startManagers();
                executorService.shutdownNow();
            } catch (Exception e) {
                stopManagers();
                executorService.schedule(this, 5, TimeUnit.SECONDS); // TODO make configurable
            }
        }
    }
}
