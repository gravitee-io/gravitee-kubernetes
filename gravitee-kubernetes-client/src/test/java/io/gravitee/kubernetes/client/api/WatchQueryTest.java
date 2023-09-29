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

import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Secret;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class WatchQueryTest {

    @Test
    void shouldWatchSingleSecret() {
        WatchQuery<Event<Secret>> query = WatchQuery.secret("my-namespace", "my-secret-name").build();

        Assertions.assertEquals(
            "/api/v1/namespaces/my-namespace/secrets?fieldSelector=metadata.name%3Dmy-secret-name&watch=true",
            query.toUri()
        );
    }

    @Test
    void shouldWatchSecretsFromAllNamespaces() {
        WatchQuery<Event<Secret>> query = WatchQuery.secrets().build();

        Assertions.assertEquals("/api/v1/secrets?watch=true", query.toUri());
    }

    @Test
    void shouldWatchSecretsFromNamespace() {
        WatchQuery<Event<Secret>> query = WatchQuery.secrets("my-namespace").build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets?watch=true", query.toUri());
    }

    @Test
    void shouldGetSecretListFromNamespaceWithBookmarks() {
        WatchQuery<Event<Secret>> query = WatchQuery.secrets("my-namespace").allowWatchBookmarks(true).build();

        Assertions.assertEquals("/api/v1/namespaces/my-namespace/secrets?watch=true&allowWatchBookmarks=true", query.toUri());
    }

    @Test
    void shouldGetSecretsFromNamespace_fieldSelector_usingFrom() {
        WatchQuery<Event<Secret>> query = WatchQuery
            .<Secret>from("/my-namespace/secrets")
            .fieldSelector(FieldSelector.equals("status.hostIP", "172.17.8.101"))
            .build();

        Assertions.assertEquals(
            "/api/v1/namespaces/my-namespace/secrets?fieldSelector=status.hostIP%3D172.17.8.101&watch=true",
            query.toUri()
        );
    }
}
