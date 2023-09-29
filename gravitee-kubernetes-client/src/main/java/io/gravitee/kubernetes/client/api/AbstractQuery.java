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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class AbstractQuery<T> {

    private static final String API_VERSION = "/api/v1";

    protected final Type type;
    protected final String namespace;
    protected final String resource;
    protected final String resourceKey;
    protected final List<FieldSelector> fieldSelectors;
    protected final List<LabelSelector> labelSelectors;

    protected AbstractQuery(
        String namespace,
        Type type,
        String resource,
        String resourceKey,
        List<FieldSelector> fieldSelectors,
        List<LabelSelector> labelSelectors
    ) {
        this.type = type;
        this.namespace = namespace;
        this.resource = resource;
        this.resourceKey = resourceKey;
        this.fieldSelectors = fieldSelectors;
        this.labelSelectors = labelSelectors;
    }

    public Class<T> getType() {
        if (singleResource()) {
            return (Class<T>) this.type.type();
        } else {
            return (Class<T>) this.type.listType();
        }
    }

    public String getResource() {
        return resource;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    private boolean singleResource() {
        return resource != null && !resource.isEmpty();
    }

    public String toUri() {
        StringBuilder builder = new StringBuilder(API_VERSION);

        // Build uri
        if (namespace != null && !namespace.isEmpty()) {
            builder.append("/namespaces/").append(namespace);
        }

        builder.append('/').append(type.value());

        if (singleResource()) {
            builder.append('/').append(resource);
        }

        // Build parameters
        List<String> parameters = buildParameters();
        if (!parameters.isEmpty()) {
            builder.append('?');
            parameters.forEach(s -> builder.append(s).append('&'));
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    protected List<String> buildParameters() {
        List<String> parameters = new ArrayList<>();

        if (!fieldSelectors.isEmpty()) {
            StringBuilder fieldSelectorBuilder = new StringBuilder("fieldSelector=");
            appendParameters(fieldSelectorBuilder, fieldSelectors);
            parameters.add(fieldSelectorBuilder.toString());
        }

        if (!labelSelectors.isEmpty()) {
            StringBuilder labelSelectorBuilder = new StringBuilder("labelSelector=");
            appendParameters(labelSelectorBuilder, labelSelectors);
            parameters.add(labelSelectorBuilder.toString());
        }

        return parameters;
    }

    private void appendParameters(StringBuilder builder, List<?> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(parameters.get(i).toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractQuery<?> that = (AbstractQuery<?>) o;
        return (
            type == that.type &&
            namespace.equals(that.namespace) &&
            Objects.equals(resource, that.resource) &&
            Objects.equals(resourceKey, that.resourceKey)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, namespace, resource, resourceKey);
    }

    public static class AbstractQueryBuilder<T> {

        protected final Type type;
        protected String namespace;
        protected String resource;
        protected String resourceKey;
        protected final List<FieldSelector> fieldSelectors = new ArrayList<>();
        protected final List<LabelSelector> labelSelectors = new ArrayList<>();

        AbstractQueryBuilder(Type type) {
            this.type = type;
        }

        public AbstractQueryBuilder<T> namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public AbstractQueryBuilder<T> fieldSelector(FieldSelector fieldSelector) {
            this.fieldSelectors.add(fieldSelector);
            return this;
        }

        public AbstractQueryBuilder<T> labelSelector(LabelSelector labelSelector) {
            this.labelSelectors.add(labelSelector);
            return this;
        }

        public AbstractQueryBuilder<T> resource(String resource) {
            this.resource = resource;
            return this;
        }

        public AbstractQueryBuilder<T> resourceKey(String resourceKey) {
            this.resourceKey = resourceKey;
            return this;
        }
    }

    protected static boolean hasEmptyValues(String[] properties) {
        for (String property : properties) {
            if (!StringUtils.hasText(property)) {
                return true;
            }
        }

        return false;
    }

    static class Reference {

        final Type type;
        final String namespace;
        final String resource;
        final String resourceKey;

        public Reference(Type type, String namespace, String resource, String resourceKey) {
            this.type = type;
            this.namespace = namespace;
            this.resource = resource;
            this.resourceKey = resourceKey;
        }

        public static Reference from(String location) {
            Objects.requireNonNull(location, "Location can not be null");
            String[] properties = location.substring(1).split("/"); // Remove the initial /

            if (properties.length < 2 || hasEmptyValues(properties)) {
                throw new IllegalArgumentException(
                    "Wrong location. A correct format looks like this \"/{namespace}/configmaps/{configmap-name}\""
                );
            }

            String resourceKey = properties.length == 4 ? properties[3] : null;
            Type resourceType = null;
            for (Type rt : Type.values()) {
                if (rt.value().equals(properties[1])) {
                    resourceType = rt;
                }
            }

            String resourceName = properties.length > 2 ? properties[2] : null;

            return new Reference(resourceType, properties[0], resourceName, resourceKey);
        }
    }
}
