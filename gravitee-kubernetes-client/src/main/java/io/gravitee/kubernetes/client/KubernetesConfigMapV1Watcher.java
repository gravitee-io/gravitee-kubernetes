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

import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since
 */
@Component
public class KubernetesConfigMapV1Watcher extends AbstractKubernetesResourceWatcher {

    @Autowired
    protected KubernetesConfigMapV1Watcher(Vertx vertx, KubernetesClient kubernetesClient, KubernetesConfig kubernetesConfig) {
        super(vertx, kubernetesClient, kubernetesConfig);
    }

    @Override
    public String urlPath(String namespace, String lastResourceVersion, String fieldSelector) {
        return (
            "/api/v1/namespaces/" +
            namespace +
            "/configmaps?" +
            "watch=true" +
            "&" +
            "allowWatchBookmarks=true" +
            "&" +
            "fieldSelector=" +
            fieldSelector +
            "&" +
            "resourceVersion=" +
            lastResourceVersion
        );
    }

    @Override
    public Single<String> retrieveLastResourceVersion(String namespace) {
        return getKubernetesClient().configMapList(namespace).map(configMapList -> configMapList.getMetadata().getResourceVersion());
    }
}
