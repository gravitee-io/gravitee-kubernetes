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
package io.gravitee.kubernetes.client.impl;

import io.gravitee.common.http.MediaType;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public class KubernetesClientV1Impl implements KubernetesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientV1Impl.class);
    private static final long PING_HANDLER_DELAY = 5000L;
    private final Vertx vertx;
    private HttpClient httpClient;
    private final Map<String, Watch> watchMap = new ConcurrentHashMap<>();

    private static final char WATCH_KEY_SEPARATOR = '#';

    public KubernetesClientV1Impl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public <T> Maybe<T> get(ResourceQuery<T> query) {
        String uri = query.toUri();
        LOGGER.debug("Retrieve resource from [{}]", uri);

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setURI(uri);
        requestOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
        return httpClient()
            .rxRequest(requestOptions)
            .flatMap(HttpClientRequest::rxSend)
            .toMaybe()
            .flatMap(
                response -> {
                    if (response.statusCode() != 200) {
                        return Maybe.error(
                            new RuntimeException(
                                String.format("Unable to retrieve resource from [%s]. Error code [%d]", uri, response.statusCode())
                            )
                        );
                    } else {
                        return response
                            .rxBody()
                            .toMaybe()
                            .flatMap(
                                buffer -> {
                                    JsonObject item = buffer.toJsonObject();
                                    T resource = item.mapTo(query.getType());
                                    if (resource != null) {
                                        return Maybe.just(resource);
                                    } else {
                                        return Maybe.empty();
                                    }
                                }
                            );
                    }
                }
            );
    }

    /** @noinspection unchecked*/
    @Override
    public <E extends Event<? extends Watchable>> Flowable<E> watch(WatchQuery<E> query) {
        String uri = query.toUri();
        String watchKey = "watch" + WATCH_KEY_SEPARATOR + uri.hashCode();

        final Watch<E> watch = watchMap.computeIfAbsent(
            watchKey,
            s -> {
                final Watch<E> w = watchEvents(watchKey, uri, query);
                w.setEvents(w.events.doFinally(() -> watchMap.remove(watchKey)));
                return w;
            }
        );

        return watch.events;
    }

    private <E extends Event<? extends Watchable>> Watch<E> watchEvents(String watchKey, String uri, WatchQuery<E> query) {
        LOGGER.debug("Start watching resources from [{}]", uri);

        final Watch<E> watch = new Watch<>(watchKey);
        final WebSocketConnectOptions webSocketConnectOptions = buildWebSocketConnectOptions(uri);

        final Flowable<E> events = Flowable
            .<E>create(
                emitter -> {
                    httpClient()
                        .rxWebSocket(webSocketConnectOptions)
                        .flatMapPublisher(
                            websocket ->
                                websocket
                                    .toFlowable()
                                    .map(response -> response.toJsonObject().mapTo((Class<E>) query.getEventType()))
                                    .doOnSubscribe(
                                        disposable ->
                                            // Periodically send a ping frame to maintain the connection up.
                                            watch.timerId =
                                                vertx.setPeriodic(
                                                    PING_HANDLER_DELAY,
                                                    aLong ->
                                                        websocket
                                                            .rxWritePing(io.vertx.reactivex.core.buffer.Buffer.buffer("ping"))
                                                            .subscribe(
                                                                () -> LOGGER.debug("Ping sent to the Kubernetes websocket"),
                                                                t -> {
                                                                    LOGGER.error("Unable to ping the Kubernetes websocket. Closing...");
                                                                    websocket.close();
                                                                    emitter.tryOnError(t);
                                                                }
                                                            )
                                                )
                                    )
                                    .doOnComplete(emitter::onComplete)
                                    .doFinally(
                                        () -> {
                                            vertx.cancelTimer(watch.timerId);
                                            websocket.close();
                                        }
                                    )
                        )
                        .subscribe(emitter::onNext, emitter::tryOnError);
                },
                BackpressureStrategy.LATEST
            )
            .doOnError(throwable -> LOGGER.error("An error occurred watching from [{}]", uri, throwable))
            .publish()
            .refCount();

        watch.setEvents(events);

        return watch;
    }

    private WebSocketConnectOptions buildWebSocketConnectOptions(String uri) {
        return new WebSocketConnectOptions()
            .setURI(uri)
            .setHost(kubeConfig().getApiServerHost())
            .setPort(kubeConfig().getApiServerPort())
            .setSsl(kubeConfig().useSSL())
            .addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
    }

    private KubernetesConfig kubeConfig() {
        return KubernetesConfig.getInstance();
    }

    private HttpClientOptions httpClientOptions() {
        PemTrustOptions trustOptions = new PemTrustOptions();
        if (kubeConfig().getCaCertData() == null || kubeConfig().getApiServerHost() == null || kubeConfig().getApiServerPort() == 0) {
            LOGGER.error(
                "KubeConfig is not configured properly. If you are running locally make sure you already configured your kubeconfig"
            );
        }

        if (kubeConfig().getCaCertData() != null) {
            trustOptions.addCertValue(Buffer.buffer(kubeConfig().getCaCertData()));
        }

        return new HttpClientOptions()
            .setTrustOptions(trustOptions)
            .setVerifyHost(kubeConfig().verifyHost())
            .setTrustAll(!kubeConfig().verifyHost())
            .setDefaultHost(kubeConfig().getApiServerHost())
            .setDefaultPort(kubeConfig().getApiServerPort())
            .setSsl(kubeConfig().useSSL());
    }

    private synchronized HttpClient httpClient() {
        if (this.httpClient == null) {
            this.httpClient = vertx.createHttpClient(httpClientOptions());
        }

        return httpClient;
    }

    private static class Watch<E extends Event<? extends Watchable>> {

        private final String key;
        private long timerId;
        private Flowable<E> events;

        public Watch(String key) {
            this.key = key;
        }

        public Flowable<E> getEvents() {
            return events;
        }

        public void setEvents(Flowable<E> events) {
            this.events = events;
        }

        public String getKey() {
            return key;
        }
    }
}
