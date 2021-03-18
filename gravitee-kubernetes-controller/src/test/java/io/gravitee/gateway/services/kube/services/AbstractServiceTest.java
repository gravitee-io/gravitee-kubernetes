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
package io.gravitee.gateway.services.kube.services;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.crds.status.GatewayResourceStatus;
import io.gravitee.gateway.services.kube.crds.status.PluginResourceStatus;
import io.gravitee.gateway.services.kube.crds.status.ServicesResourceStatus;
import io.gravitee.gateway.services.kube.utils.Fabric8sMapperUtils;
import io.gravitee.gateway.services.kube.utils.ObjectMapperHelper;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractServiceTest {

    @Autowired
    public KubernetesServer kubernetesServer;

    @Before
    public void prepare() {
        Fabric8sMapperUtils.initJsonMapper();
    }

    protected void populateSecret(String ns, String name, String filename) {
        populateSecret(ns, name, filename, 1);
    }

    protected void populateSecret(String ns, String name, String filename, int times) {
        Secret toCreate = kubernetesServer.getClient().secrets().load(getClass().getResourceAsStream(filename)).get();
        kubernetesServer.expect().get().withPath("/api/v1/namespaces/" + ns + "/secrets/" + name).andReturn(200, toCreate).times(times);
    }

    protected void populatePluginResource(String ns, String name, String filename, boolean mockStatusUpdate) {
        populatePluginResource(ns, name, filename, mockStatusUpdate, 1);
    }

    protected void populatePluginResource(String ns, String name, String filename, boolean mockStatusUpdate, int times) {
        PluginResource resource = ObjectMapperHelper.readYamlAs(filename, PluginResource.class);
        kubernetesServer
            .expect()
            .get()
            .withPath("/apis/gravitee.io/v1alpha1/namespaces/" + ns + "/gravitee-plugins/" + name)
            .andReturn(200, resource)
            .times(times);
        if (mockStatusUpdate) {
            PluginResource resourceWithStatus = ObjectMapperHelper.readYamlAs(filename, PluginResource.class);
            PluginResourceStatus status = new PluginResourceStatus();
            resourceWithStatus.setStatus(status);
            kubernetesServer
                .expect()
                .put()
                .withPath("/apis/gravitee.io/v1alpha1/namespaces/" + ns + "/gravitee-plugins/" + name + "/status")
                .andReturn(200, resourceWithStatus)
                .times(times);
        }
    }

    protected void populateGatewayResource(String ns, String name, String filename, boolean mockStatusUpdate) {
        populateGatewayResource(ns, name, filename, mockStatusUpdate, 1);
    }

    protected void populateGatewayResource(String ns, String name, String filename, boolean mockStatusUpdate, int times) {
        GatewayResource resource = ObjectMapperHelper.readYamlAs(filename, GatewayResource.class);
        kubernetesServer
            .expect()
            .get()
            .withPath("/apis/gravitee.io/v1alpha1/namespaces/" + ns + "/gravitee-gateways/" + name)
            .andReturn(200, resource)
            .times(times);
        if (mockStatusUpdate) {
            GatewayResource resourceWithStatus = ObjectMapperHelper.readYamlAs(filename, GatewayResource.class);
            GatewayResourceStatus status = new GatewayResourceStatus();
            resourceWithStatus.setStatus(status);
            kubernetesServer
                .expect()
                .put()
                .withPath("/apis/gravitee.io/v1alpha1/namespaces/" + ns + "/gravitee-gateways/" + name + "/status")
                .andReturn(200, resourceWithStatus)
                .times(times);
        }
    }

    protected void populateServicesResource(String ns, String name, String filename, boolean mockStatusUpdate) {
        populateServicesResource(ns, name, filename, mockStatusUpdate, 1);
    }

    protected void populateServicesResource(String ns, String name, String filename, boolean mockStatusUpdate, int times) {
        ServicesResource resource = ObjectMapperHelper.readYamlAs(filename, ServicesResource.class);
        kubernetesServer
            .expect()
            .get()
            .withPath("/apis/gravitee.io/v1alpha1/namespaces/" + ns + "/gravitee-services/" + name)
            .andReturn(200, resource)
            .times(times);
        if (mockStatusUpdate) {
            ServicesResource resourceWithStatus = ObjectMapperHelper.readYamlAs(filename, ServicesResource.class);
            ServicesResourceStatus status = new ServicesResourceStatus();
            resourceWithStatus.setStatus(status);
            kubernetesServer
                .expect()
                .put()
                .withPath("/apis/gravitee.io/v1alpha1/namespaces/" + ns + "/gravitee-services/" + name + "/status")
                .andReturn(200, resourceWithStatus)
                .always();
        }
    }
}
