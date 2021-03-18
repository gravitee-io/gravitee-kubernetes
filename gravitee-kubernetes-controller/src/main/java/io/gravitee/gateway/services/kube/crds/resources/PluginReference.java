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
package io.gravitee.gateway.services.kube.crds.resources;

import java.util.Objects;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PluginReference {

    public static final String DEFAULT_NAMESPACE = "default";
    private String namespace = DEFAULT_NAMESPACE;
    private String resource;
    private String name;

    public PluginReference() {}

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginReference)) return false;
        PluginReference that = (PluginReference) o;
        return namespace.equals(that.namespace) && resource.equals(that.resource) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, resource, name);
    }

    @Override
    public String toString() {
        return (
            "GraviteePluginReference{" +
            "namespace='" +
            namespace +
            '\'' +
            ", resource='" +
            resource +
            '\'' +
            ", name='" +
            name +
            '\'' +
            '}'
        );
    }
}
