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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceQuery<T> extends AbstractQuery<T> {

    protected ResourceQuery(
        String namespace,
        Type type,
        String resource,
        String resourceKey,
        List<FieldSelector> fieldSelectors,
        List<LabelSelector> labelSelectors
    ) {
        super(namespace, type, resource, resourceKey, fieldSelectors, labelSelectors);
    }

    public static QueryBuilder<ConfigMapList> configMaps() {
        return new QueryBuilder<>(Type.CONFIGMAPS);
    }

    public static QueryBuilder<ConfigMapList> configMaps(String namespace) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        return new QueryBuilder<ConfigMapList>(Type.CONFIGMAPS).namespace(namespace);
    }

    public static QueryBuilder<ConfigMap> configMap(String namespace, String configMapName) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        Objects.requireNonNull(configMapName, "ConfigMap can not be null");
        return new QueryBuilder<ConfigMap>(Type.CONFIGMAPS).namespace(namespace).resource(configMapName);
    }

    public static QueryBuilder<SecretList> secrets() {
        return new QueryBuilder<>(Type.SECRETS);
    }

    public static QueryBuilder<SecretList> secrets(String namespace) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        return new QueryBuilder<SecretList>(Type.SECRETS).namespace(namespace);
    }

    public static QueryBuilder<Secret> secret(String namespace, String secretName) {
        Objects.requireNonNull(namespace, "Namespace can not be null");
        Objects.requireNonNull(secretName, "Secret can not be null");
        return new QueryBuilder<Secret>(Type.SECRETS).namespace(namespace).resource(secretName);
    }

    public static <T> QueryBuilder<T> from(String location) {
        Reference reference = Reference.from(location);

        return new QueryBuilder<T>(reference.type)
            .namespace(reference.namespace)
            .resource(reference.resource)
            .resourceKey(reference.resourceKey);
    }

    public static class QueryBuilder<T> extends AbstractQueryBuilder<T> {

        QueryBuilder(Type type) {
            super(type);
        }

        @Override
        public QueryBuilder<T> namespace(String namespace) {
            super.namespace(namespace);
            return this;
        }

        @Override
        public QueryBuilder<T> fieldSelector(FieldSelector fieldSelector) {
            super.fieldSelector(fieldSelector);
            return this;
        }

        @Override
        public QueryBuilder<T> labelSelector(LabelSelector labelSelector) {
            super.labelSelector(labelSelector);
            return this;
        }

        @Override
        public QueryBuilder<T> resource(String resource) {
            super.resource(resource);
            return this;
        }

        @Override
        public QueryBuilder<T> resourceKey(String resourceKey) {
            super.resourceKey(resourceKey);
            return this;
        }

        public ResourceQuery<T> build() {
            return new ResourceQuery<>(namespace, type, resource, resourceKey, fieldSelectors, labelSelectors);
        }
    }
}
