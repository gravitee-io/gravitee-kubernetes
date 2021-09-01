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
import io.reactivex.Observable;
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
    protected final HttpClient httpClient;
    protected boolean stop = false;

    protected AbstractKubernetesResourceWatcher(Vertx vertx, KubernetesClient kubernetesClient, KubernetesConfig kubernetesConfig) {
        this.vertx = vertx;
        this.kubernetesClient = kubernetesClient;
        this.config = kubernetesConfig;
        this.httpClient = vertx.createHttpClient(getHttpClientOptions());
    }

    @Override
    public Observable<Event> watch(String namespace) {
        return watch(namespace, ""); // doesn't filter anything
    }

    @Override
    public Observable<Event> watch(String namespace, String fieldSelector) {
        Assert.notNull(namespace, "Namespace can't not null");
        return watch(namespace, fieldSelector, 0);
    }

    private Observable<Event> watch(String namespace, String fieldSelector, long id) {
        if (stop) {
            LOGGER.info("Kubernetes resource watcher is stopped.");
            if (id != 0) {
                vertx.cancelTimer(id);
            }
            return Observable.empty();
        }

        return retrieveLastResourceVersion(namespace)
            .flatMapObservable(lrv -> fetchEvents(namespace, lrv, fieldSelector))
            .doOnSubscribe(disposable -> vertx.setPeriodic(config.getWebsocketTimeout(), l -> watch(namespace, fieldSelector, l)))
            .doOnError(throwable -> LOGGER.error("Unable to get the last resource version", throwable));
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    private Observable<Event> fetchEvents(String namespace, String lastResourceVersion, String fieldSelector) {
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

        return httpClient
            .rxWebSocket(webSocketConnectOptions)
            .flatMapObservable(
                websocket ->
                    websocket
                        .toObservable()
                        .flatMap(
                            response -> {
                                if (stop) {
                                    LOGGER.info("Stop Kubernetes resource watcher.");
                                    websocket.close();
                                    return Observable.empty();
                                }

                                return Observable.just(response.toJsonObject().mapTo(Event.class));
                            }
                        )
                        .doFinally(
                            () -> {
                                LOGGER.debug("Close websocket connection.");
                                websocket.close();
                            }
                        )
            );
    }

    public abstract String urlPath(String namespace, String lastResourceVersion, String fieldSelector);

    public abstract Single<String> retrieveLastResourceVersion(String namespace);

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
