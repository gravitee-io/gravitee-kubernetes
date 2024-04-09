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
package io.gravitee.kubernetes.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.exception.ResourceNotFoundException;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.kubernetes.client.model.v1.Watchable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@Slf4j
public class KubernetesClientV1Impl implements KubernetesClient {

    private static final long PING_HANDLER_DELAY = 5000L;
    public static final long DISCONNECT_REPEAT_AFTER_MILLIS = 1000L;
    private static final Vertx VERTX;

    static {
        // Maintain only one dedicated instance of vertx.
        VertxOptions options = new VertxOptions();
        options.getMetricsOptions().setEnabled(false);
        VERTX = Vertx.vertx(options);
    }

    private final KubernetesConfig config;
    private HttpClient httpClient;
    private final Map<String, Watch> watchMap = new ConcurrentHashMap<>();

    private static final char WATCH_KEY_SEPARATOR = '#';

    public KubernetesClientV1Impl() {
        config = KubernetesConfig.getInstance();
    }

    public KubernetesClientV1Impl(KubernetesConfig kubeConfig) {
        this.config = kubeConfig;
    }

    @Override
    public Maybe<Watchable> create(Watchable item) {
        try {
            String uri = "";
            if (item instanceof ConfigMap configMap) {
                uri = String.format("/api/v1/namespaces/%s/configmaps", configMap.getMetadata().getNamespace());
            } else if (item instanceof Secret secret) {
                uri = String.format("/api/v1/namespaces/%s/secrets", secret.getMetadata().getNamespace());
            }

            log.debug("Create resource with uri [{}]", uri);

            RequestOptions requestOptions = getHTTPRequestOptions(HttpMethod.POST, uri);

            ObjectMapper objectMapper = new ObjectMapper();
            byte[] body = objectMapper.writeValueAsBytes(item);
            return httpClient()
                .rxRequest(requestOptions)
                .flatMap(httpClientRequest -> httpClientRequest.rxSend(io.vertx.rxjava3.core.buffer.Buffer.buffer(body)))
                .toMaybe()
                .flatMap(response -> {
                    if (
                        response.statusCode() == HttpStatusCode.OK_200 ||
                        response.statusCode() == HttpStatusCode.CREATED_201 ||
                        response.statusCode() == HttpStatusCode.ACCEPTED_202
                    ) {
                        return response
                            .rxBody()
                            .toMaybe()
                            .flatMap(buffer -> {
                                JsonObject object = buffer.toJsonObject();
                                Watchable resource = object.mapTo(item.getClass());
                                if (resource != null) {
                                    return Maybe.just(resource);
                                } else {
                                    return Maybe.empty();
                                }
                            });
                    } else {
                        return Maybe.error(
                            new RuntimeException(String.format("Unable to create resource in. Error code [%d]", response.statusCode()))
                        );
                    }
                });
        } catch (Exception e) {
            return Maybe.error(new RuntimeException(String.format("Unable to create resource in. %s", e.getMessage())));
        }
    }

    @Override
    public <T> Maybe<T> get(ResourceQuery<T> query) {
        String uri = query.toUri();
        log.debug("Retrieve resource from [{}]", uri);

        RequestOptions requestOptions = getHTTPRequestOptions(HttpMethod.GET, uri);
        return httpClient()
            .rxRequest(requestOptions)
            .flatMap(HttpClientRequest::rxSend)
            .toMaybe()
            .flatMap(response -> {
                if (response.statusCode() != 200) {
                    if (response.statusCode() == 404) {
                        return Maybe.error(new ResourceNotFoundException("Can't find resource at " + uri));
                    }

                    return Maybe.error(
                        new RuntimeException(
                            String.format("Unable to retrieve resource from [%s]. Error code [%d]", uri, response.statusCode())
                        )
                    );
                } else {
                    return response
                        .rxBody()
                        .toMaybe()
                        .flatMap(buffer -> {
                            JsonObject item = buffer.toJsonObject();
                            T resource = item.mapTo(query.getType());
                            if (resource != null) {
                                return Maybe.just(resource);
                            } else {
                                return Maybe.empty();
                            }
                        });
                }
            });
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
        log.debug("Start watching resources from [{}]", uri);

        final Watch<E> watch = new Watch<>(watchKey);
        final WebSocketConnectOptions webSocketConnectOptions = buildWebSocketConnectOptions(uri);

        final Flowable<E> events = httpClient()
            .rxWebSocket(webSocketConnectOptions)
            .flatMapPublisher(websocket -> {
                Flowable<E> pingFlowable = websocketPing(websocket);
                return pingFlowable.compose(
                    mergeWithFirst(websocket.toFlowable().map(response -> response.toJsonObject().mapTo((Class<E>) query.getEventType())))
                );
            })
            .doOnError(throwable -> log.error("An error occurred watching from [{}]", uri, throwable))
            .publish()
            .refCount()
            .doOnTerminate(() -> log.debug("reconnecting due to websocket termination at [{}]", uri))
            .repeatWhen(p -> p.delay(DISCONNECT_REPEAT_AFTER_MILLIS, TimeUnit.MILLISECONDS));

        watch.setEvents(events);

        return watch;
    }

    private <E> Flowable<E> websocketPing(io.vertx.rxjava3.core.http.WebSocket webSocket) {
        return Flowable
            .interval(PING_HANDLER_DELAY, TimeUnit.MILLISECONDS)
            .timestamp()
            .flatMapCompletable(timed -> webSocket.rxWritePing(io.vertx.rxjava3.core.buffer.Buffer.buffer("ping")))
            .doOnError(throwable -> log.error("An error occurred while sending ping to websocket", throwable))
            .toFlowable();
    }

    private <E> FlowableTransformer<E, E> mergeWithFirst(Flowable<E> other) {
        return upstream -> other.materialize().mergeWith(upstream.materialize()).dematerialize(n -> n);
    }

    private RequestOptions getHTTPRequestOptions(HttpMethod post, String uri) {
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(post);
        requestOptions.setURI(uri);
        requestOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        if (kubeConfig().getAccessToken() != null && !kubeConfig().getAccessToken().isBlank()) {
            requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
        }
        return requestOptions;
    }

    private WebSocketConnectOptions buildWebSocketConnectOptions(String uri) {
        WebSocketConnectOptions options = new WebSocketConnectOptions()
            .setURI(uri)
            .setHost(kubeConfig().getApiServerHost())
            .setPort(kubeConfig().getApiServerPort())
            .setSsl(kubeConfig().useSSL())
            .addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        if (kubeConfig().getAccessToken() != null && !kubeConfig().getAccessToken().isBlank()) {
            options.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
        }
        return options;
    }

    private KubernetesConfig kubeConfig() {
        return this.config;
    }

    private HttpClientOptions httpClientOptions() {
        PemTrustOptions trustOptions = new PemTrustOptions();
        if (kubeConfig().getCaCertData() == null || kubeConfig().getApiServerHost() == null || kubeConfig().getApiServerPort() == 0) {
            log.error("KubeConfig is not configured properly. If you are running locally make sure you already configured your kubeconfig");
        }

        if (kubeConfig().getCaCertData() != null) {
            trustOptions.addCertValue(Buffer.buffer(kubeConfig().getCaCertData()));
        }

        HttpClientOptions opt = new HttpClientOptions();

        if (kubeConfig().getClientCertData() != null && kubeConfig().getClientKeyData() != null) {
            // setup mTLS (create a keystore and export and set again as buffer for vertx)
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                final String password = "";
                final String alias = "default";
                KeyStore keyStore = KeyStoreUtils.initFromPem(
                    kubeConfig().getClientCertData(),
                    kubeConfig().getClientKeyData(),
                    password,
                    alias
                );
                keyStore.store(output, password.toCharArray());
                opt.setKeyStoreOptions(
                    new JksOptions().setPassword(password).setAlias(alias).setValue(Buffer.buffer(output.toByteArray()))
                );
            } catch (Exception e) {
                log.warn("Client certificate configuration failed", e);
            }
        }

        return opt
            .setTrustOptions(trustOptions)
            .setVerifyHost(kubeConfig().verifyHost())
            .setTrustAll(!kubeConfig().verifyHost())
            .setDefaultHost(kubeConfig().getApiServerHost())
            .setDefaultPort(kubeConfig().getApiServerPort())
            .setConnectTimeout(kubeConfig().getApiTimeout())
            .setSsl(kubeConfig().useSSL());
    }

    public synchronized HttpClient httpClient() {
        if (this.httpClient == null) {
            this.httpClient = VERTX.createHttpClient(httpClientOptions());
        }

        return httpClient;
    }

    private static class Watch<E extends Event<? extends Watchable>> {

        private final String key;
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
