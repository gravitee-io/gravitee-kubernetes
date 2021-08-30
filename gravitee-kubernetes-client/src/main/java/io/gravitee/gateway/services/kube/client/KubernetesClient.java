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
package io.gravitee.gateway.services.kube.client;

import io.gravitee.gateway.services.kube.client.model.v1.ConfigMap;
import io.gravitee.gateway.services.kube.client.model.v1.ConfigMapList;
import io.gravitee.gateway.services.kube.client.model.v1.Secret;
import io.gravitee.gateway.services.kube.client.model.v1.SecretList;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public interface KubernetesClient {
    /**
     * @param namespace name
     * @return list of {@link Secret} in the given namespace
     */
    Single<SecretList> secretList(String namespace);

    /**
     * Retrieve Kubernetes secret with given name
     * @param namespace
     * @param secretName
     * @return the {@link Secret} info if it exist
     */
    Maybe<Secret> secret(String namespace, String secretName);

    /**
     * @param namespace
     * @return list of {@link ConfigMapList}
     */
    Single<ConfigMapList> configMapList(String namespace);

    /**
     * Retrieve Kubernetes config map with give name
     * @param namespace
     * @param configMapName
     * @return the {@link ConfigMap} if it exist
     */
    Maybe<ConfigMap> configMap(String namespace, String configMapName);
}
