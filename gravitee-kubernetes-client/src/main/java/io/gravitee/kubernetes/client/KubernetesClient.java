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

import io.gravitee.kubernetes.client.model.v1.ConfigMapList;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.kubernetes.client.model.v1.SecretList;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Future;

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
     * @param namespace
     * @return list of {@link ConfigMapList}
     */
    Single<ConfigMapList> configMapList(String namespace);

    /**
     * Get a Kubernetes element for the given location.
     *
     * @param location the location of the Kubernetes element. Example:
     *    kube://default/configmap/gravitee-config
     * @return the expected element or nothing if it does not exists.
     */
    <T> Maybe<T> get(String location, Class<T> type);

    /**
     * Watch for any changes on a Kubernetes element at a given location. Example:
     *      kube://default/secret/gravitee-config
     * @return a flowable where element will be pushed at any change.
     */
    <T extends Event<?>> Flowable<T> watch(String location, Class<T> type);

    Future<Void> stop();
}
