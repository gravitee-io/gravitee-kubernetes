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
package io.gravitee.kubernetes.client.api;

import io.gravitee.kubernetes.client.model.v1.*;
import java.util.List;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WatchQuery<E extends Event<? extends Watchable>> extends AbstractQuery<E> {

    private final boolean allowWatchBookmarks;

    protected WatchQuery(
        String namespace,
        Type type,
        String resource,
        String resourceKey,
        List<FieldSelector> fieldSelectors,
        List<LabelSelector> labelSelectors,
        boolean allowWatchBookmarks
    ) {
        super(namespace, type, resource, resourceKey, fieldSelectors, labelSelectors);
        this.allowWatchBookmarks = allowWatchBookmarks;
    }

    public static WatchQueryBuilder<ConfigMap, Event<ConfigMap>> configMaps() {
        return new WatchQueryBuilder<>(Type.CONFIGMAPS);
    }

    public static WatchQueryBuilder<ConfigMap, Event<ConfigMap>> configMaps(String namespace) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        return new WatchQueryBuilder<ConfigMap, Event<ConfigMap>>(Type.CONFIGMAPS).namespace(namespace);
    }

    public static WatchQueryBuilder<ConfigMap, Event<ConfigMap>> configMap(String namespace, String configMapName) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        Objects.requireNonNull(configMapName, "ConfigMap can not be null");
        return new WatchQueryBuilder<ConfigMap, Event<ConfigMap>>(Type.CONFIGMAPS).namespace(namespace).resource(configMapName);
    }

    public static WatchQueryBuilder<Secret, Event<Secret>> secrets() {
        return new WatchQueryBuilder<>(Type.SECRETS);
    }

    public static WatchQueryBuilder<Secret, Event<Secret>> secrets(String namespace) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        return new WatchQueryBuilder<Secret, Event<Secret>>(Type.SECRETS).namespace(namespace);
    }

    public static WatchQueryBuilder<Secret, Event<Secret>> secret(String namespace, String secretName) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        Objects.requireNonNull(secretName, "Secret can not be null");
        return new WatchQueryBuilder<Secret, Event<Secret>>(Type.SECRETS).namespace(namespace).resource(secretName);
    }

    public static <T extends Watchable> WatchQueryBuilder<T, Event<T>> from(String location) {
        Reference reference = Reference.from(location);

        return new WatchQueryBuilder<T, Event<T>>(reference.type)
            .namespace(reference.namespace)
            .resource(reference.resource)
            .resourceKey(reference.resourceKey);
    }

    @Override
    protected List<String> buildParameters() {
        List<String> parameters = super.buildParameters();
        parameters.add("watch=true");

        if (allowWatchBookmarks) {
            parameters.add("allowWatchBookmarks=true");
        }

        return parameters;
    }

    public Class<?> getEventType() {
        return type.eventType();
    }

    public static class WatchQueryBuilder<T extends Watchable, E extends Event<T>> extends AbstractQueryBuilder<T> {

        private boolean allowWatchBookmarks = false;

        WatchQueryBuilder(Type type) {
            super(type);
        }

        @Override
        public WatchQueryBuilder<T, E> namespace(String namespace) {
            super.namespace(namespace);
            return this;
        }

        @Override
        public WatchQueryBuilder<T, E> fieldSelector(FieldSelector fieldSelector) {
            super.fieldSelector(fieldSelector);
            return this;
        }

        @Override
        public WatchQueryBuilder<T, E> labelSelector(LabelSelector labelSelector) {
            super.labelSelector(labelSelector);
            return this;
        }

        @Override
        public WatchQueryBuilder<T, E> resource(String resource) {
            super.resource(resource);
            return this;
        }

        @Override
        public WatchQueryBuilder<T, E> resourceKey(String resourceKey) {
            super.resourceKey(resourceKey);
            return this;
        }

        public WatchQueryBuilder<T, E> allowWatchBookmarks(boolean allowWatchBookmarks) {
            this.allowWatchBookmarks = allowWatchBookmarks;
            return this;
        }

        public WatchQuery<E> build() {
            return new WatchQuery<>(namespace, type, resource, resourceKey, fieldSelectors, labelSelectors, allowWatchBookmarks);
        }
    }
}
