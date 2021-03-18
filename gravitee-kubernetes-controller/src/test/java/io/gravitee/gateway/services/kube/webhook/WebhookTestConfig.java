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
package io.gravitee.gateway.services.kube.webhook;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.ServicesResourceService;
import io.gravitee.gateway.services.kube.webhook.validator.ResourceValidatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class WebhookTestConfig {

    @Bean
    public PluginsResourceService pluginsResourceService() {
        return mock(PluginsResourceService.class);
    }

    @Bean
    public GatewayResourceService gatewayResourceService() {
        return mock(GatewayResourceService.class);
    }

    @Bean
    public ServicesResourceService servicesResourceService() {
        return mock(ServicesResourceService.class);
    }

    @Bean
    public ResourceValidatorFactory resourceValidatorFactory() {
        return spy(new ResourceValidatorFactory());
    }

    @Bean
    public AdmissionWebHook webhook() {
        return new AdmissionWebHook();
    }
}
