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

import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.kubernetes.client.model.v1.SecretList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResourceQueryTest {

    @Test
    void should_get_single_secret() {
        ResourceQuery<Secret> query = ResourceQuery.secret("my-namespace", "my-secret-name").build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets/my-secret-name", query.toUri());
    }

    @Test
    void should_get_single_secret_using_from() {
        ResourceQuery<Secret> query = ResourceQuery.<Secret>from("/my-namespace/secrets/my-secret-name").build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets/my-secret-name", query.toUri());
    }

    @Test
    void should_get_secrets_from_namespace() {
        ResourceQuery<SecretList> query = ResourceQuery.secrets("my-namespace").build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets", query.toUri());
    }

    @Test
    void should_get_secrets_from_namespace_field_selector() {
        ResourceQuery<SecretList> query = ResourceQuery
            .secrets("my-namespace")
            .fieldSelector(FieldSelector.equals("status.hostIP", "172.17.8.101"))
            .build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets?fieldSelector=status.hostIP%3D172.17.8.101", query.toUri());
    }

    @Test
    void should_get_secrets_from_namespace_field_selector_using_from() {
        ResourceQuery<SecretList> query = ResourceQuery
            .<SecretList>from("/my-namespace/secrets")
            .fieldSelector(FieldSelector.equals("status.hostIP", "172.17.8.101"))
            .build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets?fieldSelector=status.hostIP%3D172.17.8.101", query.toUri());
    }
}
