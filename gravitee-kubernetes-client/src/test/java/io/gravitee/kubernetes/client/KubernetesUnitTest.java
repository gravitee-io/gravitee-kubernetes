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
package io.gravitee.kubernetes.client;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.impl.KubernetesClientV1Impl;
import io.vertx.reactivex.core.Vertx;
import org.junit.After;
import org.junit.Before;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class KubernetesUnitTest {

    protected static final Long EVENT_WAIT_PERIOD_MS = 10L;
    protected Vertx vertx = Vertx.vertx();
    protected KubernetesMockServer server;
    protected KubernetesClient kubernetesClient;

    @Before
    public void before() {
        server = new KubernetesMockServer(true);
        server.init();

        // Set kubeconfig system property to use for creating a
        buildKubernetesConfig();
        kubernetesClient = new KubernetesClientV1Impl(vertx);
    }

    @After
    public void after() {
        vertx.exceptionHandler(null);
        //    kubernetesClient.stop();
        server.destroy();
        vertx.close();
    }

    // Helper methods
    protected void buildKubernetesConfig() {
        KubernetesConfig config = KubernetesConfig.getInstance();
        config.setApiServerHost(server.getHostName());
        config.setApiServerPort(server.getPort());
        config.setVerifyHost(false);
    }
}
