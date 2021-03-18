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
package io.gravitee.gateway.services.kube.crds;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ResourceConstants {
    String GROUP = "gravitee.io";
    String V1ALPHA1_VERSION = "v1alpha1";
    String DEFAULT_VERSION = V1ALPHA1_VERSION;
    String SCOPE = "Namespaced";

    // Plugins
    String PLUGINS_FULLNAME = "gravitee-plugins.gravitee.io";
    String PLUGINS_PLURAL = "gravitee-plugins";
    String PLUGINS_KIND = "GraviteePlugins";

    // Gateway
    String GATEWAY_FULLNAME = "gravitee-gateways.gravitee.io";
    String GATEWAY_PLURAL = "gravitee-gateways";
    String GATEWAY_KIND = "GraviteeGateway";

    // Services
    String SERVICES_FULLNAME = "gravitee-services.gravitee.io";
    String SERVICES_PLURAL = "gravitee-services";
    String SERVICES_KIND = "GraviteeServices";
}
