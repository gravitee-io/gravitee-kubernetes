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
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public abstract class AbstractKubernetesResourceWatcher implements KubernetesResourceWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKubernetesResourceWatcher.class);

    protected final Vertx vertx;
    protected final KubernetesClient kubernetesClient;
    protected final KubernetesConfig config;
    protected boolean stop = false;
    private HttpClient httpClient;

    protected AbstractKubernetesResourceWatcher(Vertx vertx, KubernetesClient kubernetesClient, KubernetesConfig kubernetesConfig) {
        this.vertx = vertx;
        this.kubernetesClient = kubernetesClient;
        this.config = kubernetesConfig;
    }

    @Override
    public void watch(String namespace) {
        watch(namespace, ""); // doesn't filter anything
    }

    @Override
    public void watch(String namespace, String fieldSelector) {
        Assert.notNull(namespace, "Namespace can't not null");

        if (stop) {
            LOGGER.info("Kubernetes resource watcher is stopped.");
            httpClient.close();
            return;
        }

        retrieveLastResourceVersion(namespace)
            .doOnSuccess(
                lrv -> {
                    httpClient = vertx.createHttpClient(getHttpClientOptions());
                    fetchEvents(namespace, lrv, fieldSelector);
                    vertx.setTimer(
                        config.getWebsocketTimeout(),
                        l -> {
                            httpClient.close();
                            watch(namespace, fieldSelector);
                        }
                    );
                }
            )
            .doOnError(throwable -> LOGGER.error("Unable to get the last resource version", throwable))
            .subscribe();
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    private void fetchEvents(String namespace, String lastResourceVersion, String fieldSelector) {
        if (fieldSelector == null) {
            LOGGER.warn("Invalid fieldSelector value! [null] It will be ignored...");
            fieldSelector = "";
        }

        WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
        webSocketConnectOptions.setURI(urlPath(namespace, lastResourceVersion, fieldSelector));
        webSocketConnectOptions.setHost(config.getApiServerHost());
        webSocketConnectOptions.setPort(config.getApiServerPort());
        webSocketConnectOptions.setSsl(true);
        webSocketConnectOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        webSocketConnectOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getServiceAccountToken());

        httpClient
            .rxWebSocket(webSocketConnectOptions)
            .doOnSuccess(
                websocket ->
                    websocket.handler(
                        response -> {
                            if (stop) {
                                LOGGER.info("Stop Kubernetes resource watcher.");
                                httpClient.close();
                                return;
                            }
                            Event event = response.toJsonObject().mapTo(Event.class);
                            eventReceived(event);
                        }
                    )
            )
            .doOnError(
                throwable ->
                    LOGGER.error("Error connecting host {}, port {}", config.getApiServerHost(), config.getApiServerPort(), throwable)
            )
            .subscribe();
    }

    public abstract String urlPath(String namespace, String lastResourceVersion, String fieldSelector);

    public abstract Single<String> retrieveLastResourceVersion(String namespace);

    public abstract void eventReceived(Event event);

    protected HttpClientOptions getHttpClientOptions() {
        PemTrustOptions trustOptions = new PemTrustOptions();
        trustOptions.addCertValue(Buffer.buffer(config.getCaCertData()));

        return new HttpClientOptions()
            .setTrustOptions(trustOptions)
            .setVerifyHost(config.verifyHost())
            .setTrustAll(!config.verifyHost())
            .setDefaultHost(config.getApiServerHost())
            .setDefaultPort(config.getApiServerPort())
            .setSsl(config.useSSL());
    }
}
