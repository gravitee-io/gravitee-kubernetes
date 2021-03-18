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

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.gravitee.gateway.services.kube.crds.cache.PluginRevision;
import io.gravitee.gateway.services.kube.crds.resources.*;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.services.listeners.GatewayResourceListener;
import io.reactivex.Flowable;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GatewayResourceService {
    void registerListener(GatewayResourceListener listener);

    List<GatewayResource> listAllGateways();

    /**
     * Check if the GraviteeGateway definition may be safely created (no missing secret for example)
     * @param gateway
     * @throws io.gravitee.gateway.services.kube.exceptions.ValidationException in case of validation error
     */
    void maybeSafelyCreated(GatewayResource gateway);

    /**
     * Check if the GraviteeGateway definition may be safely updated (no deletion of resources currently in used by an API)
     * @param gateway
     * @throws io.gravitee.gateway.services.kube.exceptions.ValidationException in case of validation error
     */
    void maybeSafelyUpdated(GatewayResource gateway);

    /**
     * Check if the GraviteeGateway definition may be safely deleted
     * @param gateway
     * @throws io.gravitee.gateway.services.kube.exceptions.ValidationException in case of validation error
     */
    void maybeSafelyDeleted(GatewayResource gateway);

    Flowable<WatchActionContext<GatewayResource>> processAction(WatchActionContext<GatewayResource> context);

    WatchActionContext<GatewayResource> persistAsSuccess(WatchActionContext<GatewayResource> context);
    WatchActionContext<GatewayResource> persistAsError(WatchActionContext<GatewayResource> context, String message);

    MixedOperation<GatewayResource, GatewayResourceList, DoneableGatewayResource, Resource<GatewayResource, DoneableGatewayResource>> getCrdClient();

    GatewayResource lookup(WatchActionContext<?> context, GatewayResourceReference ref);

    List<PluginRevision<io.gravitee.definition.model.plugins.resources.Resource>> extractResources(
        WatchActionContext<GatewayResource> context
    );
}
