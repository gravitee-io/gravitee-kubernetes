/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.kubernetes.client;

import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public interface KubernetesClient {
    /**
     * Creates a watchable item in the given namespace.
     *
     * @param  item  a watchable item (such a Secret or ConfigMap)
     * @return the saved item if there was no error
     */
    Maybe<Watchable> create(Watchable item);

    /**
     * Get an item given a resource query. Resource query can be just the name and the namespace of the
     * resource or it can be also a label
     *
     * @param  query that is used to get a resource from the cluster
     * @return a watchable item if it exist
     */
    <T> Maybe<T> get(ResourceQuery<T> query);

    /**
     * Watch for different events (such as ADD, UPDATE, DELETE) for a specific resource in the cluster
     * This method will create and maintain a websocket connection to the API server
     *
     * @param  query that is used to get a resource from the cluster
     * @return a watchable item if it exist
     */
    <E extends Event<? extends Watchable>> Flowable<E> watch(WatchQuery<E> query);
}
