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
package io.gravitee.gateway.services.kube.webhook.validator;

import static java.lang.String.format;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.exceptions.ValidationException;
import io.gravitee.gateway.services.kube.services.ServicesResourceService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServicesValidator implements ResourceValidator {

    private final ServicesResource oldServices;
    private final ServicesResource services;

    @Autowired
    protected ServicesResourceService servicesService;

    public ServicesValidator(KubernetesResource oldServices, KubernetesResource services) {
        this.oldServices = (ServicesResource) oldServices;
        this.services = (ServicesResource) services;
    }

    @Override
    public Status validate(String operation) {
        final Operation op = Operation.valueOf(operation.toUpperCase());
        final StatusBuilder builder = new StatusBuilder();
        try {
            switch (op) {
                case CREATE:
                case UPDATE:
                    servicesService.maybeSafelyUpdated(services);
                    builder.withCode(200);
                    break;
                case DELETE:
                    // there are no dependencies on Services, authorize the deletion
                    builder.withCode(200);
                    break;
                default:
                    builder.withCode(400).withMessage(format("Operation '%s' not managed", operation));
                    break;
            }
        } catch (ValidationException e) {
            builder.withCode(400).withMessage(e.getMessage());
        }
        return builder.build();
    }
}
