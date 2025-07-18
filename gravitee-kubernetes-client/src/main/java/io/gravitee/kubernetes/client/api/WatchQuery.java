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
package io.gravitee.kubernetes.client.api;

import io.gravitee.kubernetes.client.model.v1.*;
import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(callSuper = true)
public class WatchQuery<E extends Event<? extends Watchable>> extends AbstractQuery<E> {

    public static final String NAMESPACE_CAN_NOT_BE_NULL = "Namespace can not be null";
    private final boolean allowWatchBookmarks;

    protected WatchQuery(
        String namespace,
        Type type,
        String resource,
        String resourceKey,
        String resourceVersion,
        List<FieldSelector> fieldSelectors,
        List<LabelSelector> labelSelectors,
        boolean allowWatchBookmarks
    ) {
        super(namespace, type, resource, resourceKey, resourceVersion, fieldSelectors, labelSelectors);
        this.allowWatchBookmarks = allowWatchBookmarks;
    }

    public static WatchQueryBuilder<ConfigMap, Event<ConfigMap>> configMaps() {
        return new WatchQueryBuilder<>(Type.CONFIGMAPS);
    }

    public static WatchQueryBuilder<ConfigMap, Event<ConfigMap>> configMaps(String namespace) {
        Objects.requireNonNull(namespace, NAMESPACE_CAN_NOT_BE_NULL);
        return new WatchQueryBuilder<ConfigMap, Event<ConfigMap>>(Type.CONFIGMAPS).namespace(namespace);
    }

    public static WatchQueryBuilder<ConfigMap, Event<ConfigMap>> configMap(String namespace, String configMapName) {
        Objects.requireNonNull(namespace, NAMESPACE_CAN_NOT_BE_NULL);
        Objects.requireNonNull(configMapName, "ConfigMap can not be null");
        return new WatchQueryBuilder<ConfigMap, Event<ConfigMap>>(Type.CONFIGMAPS).namespace(namespace).resource(configMapName);
    }

    public static WatchQueryBuilder<Secret, Event<Secret>> secrets() {
        return new WatchQueryBuilder<>(Type.SECRETS);
    }

    public static WatchQueryBuilder<Secret, Event<Secret>> secrets(String namespace) {
        Objects.requireNonNull(namespace, NAMESPACE_CAN_NOT_BE_NULL);
        return new WatchQueryBuilder<Secret, Event<Secret>>(Type.SECRETS).namespace(namespace);
    }

    public static WatchQueryBuilder<Secret, Event<Secret>> secret(String namespace, String secretName) {
        Objects.requireNonNull(namespace, NAMESPACE_CAN_NOT_BE_NULL);
        Objects.requireNonNull(secretName, "Secret can not be null");

        return secrets(namespace).resource(secretName);
    }

    public static <T extends Watchable> WatchQueryBuilder<T, Event<T>> from(String location) {
        Reference reference = Reference.from(location);

        if (reference.resource == null) {
            return new WatchQueryBuilder<T, Event<T>>(reference.type).namespace(reference.namespace);
        }

        return new WatchQueryBuilder<T, Event<T>>(reference.type).namespace(reference.namespace).resource(reference.resource);
    }

    @Override
    protected String uriResource() {
        // k8s does not allow for watching a specific resource in uri (need to use field selector metadata.name).
        return "";
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

    public Class<? extends Event<Watchable>> getEventType() {
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
            if (resource != null && !resource.contains("*")) {
                fieldSelector(FieldSelector.equals("metadata.name", resource));
            }
            return this;
        }

        @Override
        public WatchQueryBuilder<T, E> resourceKey(String resourceKey) {
            super.resourceKey(resourceKey);
            return this;
        }

        @Override
        public WatchQueryBuilder<T, E> resourceVersion(String resourceVersion) {
            super.resourceVersion(resourceVersion);
            return this;
        }

        public WatchQueryBuilder<T, E> allowWatchBookmarks(boolean allowWatchBookmarks) {
            this.allowWatchBookmarks = allowWatchBookmarks;
            return this;
        }

        public WatchQuery<E> build() {
            return new WatchQuery<>(
                namespace,
                type,
                resource,
                resourceKey,
                resourceVersion,
                fieldSelectors,
                labelSelectors,
                allowWatchBookmarks
            );
        }
    }
}
