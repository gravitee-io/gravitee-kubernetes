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

import static io.gravitee.gateway.services.kube.crds.ResourceConstants.*;

import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ResourceValidatorFactory implements ApplicationContextAware {

    private ApplicationContext appContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    public ResourceValidator getValidator(AdmissionRequest request) {
        final String kind = request.getRequestKind().getKind();
        ResourceValidator validator;
        switch (kind) {
            case PLUGINS_KIND:
                validator = new PluginsValidator(request.getOldObject(), request.getObject());
                break;
            case GATEWAY_KIND:
                validator = new GatewayValidator(request.getOldObject(), request.getObject());
                break;
            case SERVICES_KIND:
                validator = new ServicesValidator(request.getOldObject(), request.getObject());
                break;
            default:
                // this should never happen because the kind is used to deserialize object
                validator = new KindUnknownValidator(kind);
        }

        appContext.getAutowireCapableBeanFactory().autowireBean(validator);
        return validator;
    }
}
