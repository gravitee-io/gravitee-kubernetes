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
import io.gravitee.gateway.services.kube.crds.resources.DoneableServicesResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResourceList;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.reactivex.Flowable;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ServicesResourceService {
    Flowable<WatchActionContext<ServicesResource>> processAction(WatchActionContext<ServicesResource> context);

    List<ServicesResource> listAllServices();

    MixedOperation<ServicesResource, ServicesResourceList, DoneableServicesResource, Resource<ServicesResource, DoneableServicesResource>> getCrdClient();

    /**
     * Check if the GraviteeServices definition may be safely updated
     * @param services
     * @throws io.gravitee.gateway.services.kube.exceptions.ValidationException in case of validation error
     */
    void maybeSafelyUpdated(ServicesResource services);

    WatchActionContext<ServicesResource> persistAsError(WatchActionContext<ServicesResource> context, String message);
}
