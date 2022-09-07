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
package io.gravitee.kubernetes.mapper;

/**
 * @author GraviteeSource Team
 */
public enum GroupVersionKind {
    GIO_V1_ALPHA_1_API_DEFINITION("gravitee.io", "v1alpha1", "ApiDefinition");

    private final String group;
    private final String version;
    private final String kind;

    GroupVersionKind(String group, String version, String kind) {
        this.group = group;
        this.version = version;
        this.kind = kind;
    }

    public String group() {
        return group;
    }

    public String version() {
        return version;
    }

    public String kind() {
        return kind;
    }
}
