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
package io.gravitee.gateway.services.kube.client;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.services.kube.client.config.KubernetesConfig;
import io.gravitee.gateway.services.kube.client.model.v1.Event;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public abstract class AbstractKubernetesWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKubernetesWatcher.class);

    private final Vertx vertx;
    private final KubernetesClient kubernetesClient;
    private final KubernetesConfig config;

    public AbstractKubernetesWatcher(Vertx vertx, KubernetesClient kubernetesClient, KubernetesConfig kubernetesConfig) {
        this.vertx = vertx;
        this.kubernetesClient = kubernetesClient;
        this.config = kubernetesConfig;
    }

    public void watch(String namespace) {
        watch(namespace, (e -> true)); // doesn't filter anything
    }

    public void watch(String namespace, Predicate<Event> resourceNamePredicate) {
        retrieveLastResourceVersion(namespace)
            .doOnSuccess(
                lrv -> {
                    AtomicReference<HttpClient> clientAtomicReference = new AtomicReference<>(
                        fetchEvents(vertx.createHttpClient(getHttpClientOptions()), namespace, lrv, resourceNamePredicate)
                    );
                    vertx.setPeriodic(
                        config.getWebsocketTimeout(),
                        l -> {
                            if (clientAtomicReference.get() != null) {
                                clientAtomicReference.get().close();
                            }

                            clientAtomicReference.set(
                                fetchEvents(vertx.createHttpClient(getHttpClientOptions()), namespace, lrv, resourceNamePredicate)
                            );
                        }
                    );
                }
            )
            .doOnError(throwable -> LOGGER.error("Unable to retrieve Kubernetes config maps", throwable))
            .subscribe();
    }

    private HttpClient fetchEvents(
        HttpClient client,
        String namespace,
        String lastResourceVersion,
        Predicate<Event> resourceNamePredicate
    ) {
        WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
        webSocketConnectOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        webSocketConnectOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getServiceAccountToken());
        webSocketConnectOptions.setURI(urlPath(namespace, lastResourceVersion));

        client
            .rxWebSocket(webSocketConnectOptions)
            .doOnSuccess(
                websocket ->
                    websocket.handler(
                        response -> {
                            Event event = response.toJsonObject().mapTo(Event.class);
                            if (resourceNamePredicate.test(event)) {
                                eventReceived(event);
                            }
                        }
                    )
            )
            .doOnError(throwable -> LOGGER.error("Error connection got Kubernetes web socket", throwable))
            .subscribe();

        return client;
    }

    public abstract String urlPath(String namespace, String lastResourceVersion);

    public abstract Single<String> retrieveLastResourceVersion(String namespace);

    public abstract void eventReceived(Event event);

    protected HttpClientOptions getHttpClientOptions() {
        JksOptions jksOptions = new JksOptions().setValue(Buffer.buffer(config.getCaTrustStore())).setPassword("");

        return new HttpClientOptions()
            .setKeyStoreOptions(jksOptions)
            .setVerifyHost(false)
            .setTrustAll(true)
            .setDefaultHost(config.getApiServerHost())
            .setDefaultPort(config.getApiServerPort())
            .setProtocolVersion(HttpVersion.HTTP_2)
            .setHttp2ClearTextUpgrade(false)
            .setSsl(true);
    }

    // property methods
    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    public KubernetesConfig getConfig() {
        return config;
    }
}
