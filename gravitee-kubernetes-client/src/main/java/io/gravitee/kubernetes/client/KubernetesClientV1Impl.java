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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.exception.ResourceNotFoundException;
import io.gravitee.kubernetes.client.model.v1.*;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

    @Autowired
    public KubernetesClientV1Impl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Single<SecretList> secretList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

        LOGGER.debug("Retrieve list of secrets in namespace [{}]", namespace);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setURI(String.format("/api/v1/namespaces/%s/secrets", namespace));
        requestOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
        return httpClient()
            .rxRequest(requestOptions)
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(
                response -> {
                    if (response.statusCode() != 200) {
                        return Single.error(
                            new RuntimeException(
                                String.format(
                                    "Unable to retrieve list of secrets from namespace %s. error code %d",
                                    namespace,
                                    response.statusCode()
                                )
                            )
                        );
                    } else {
                        return response
                            .rxBody()
                            .flatMap(
                                buffer -> {
                                    SecretList secretList = buffer.toJsonObject().mapTo(SecretList.class);
                                    if (secretList == null) {
                                        return Single.error(
                                            new ResourceNotFoundException(
                                                String.format("Unable to retrieve list of secrets from namespace %s", namespace)
                                            )
                                        );
                                    } else {
                                        return Single.just(secretList);
                                    }
                                }
                            );
                    }
                }
            );
    }

    @Override
    public Single<ConfigMapList> configMapList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

        LOGGER.debug("Retrieve list of configmaps in namespace [{}]", namespace);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setURI(String.format("/api/v1/namespaces/%s/configmaps", namespace));
        requestOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
        return httpClient()
            .rxRequest(requestOptions)
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(
                response -> {
                    if (response.statusCode() != 200) {
                        return Single.error(
                            new RuntimeException(
                                String.format(
                                    "Unable to retrieve list of configmaps from namespace %s. error code %d",
                                    namespace,
                                    response.statusCode()
                                )
                            )
                        );
                    } else {
                        return response
                            .rxBody()
                            .flatMap(
                                buffer -> {
                                    ConfigMapList configMapList = buffer.toJsonObject().mapTo(ConfigMapList.class);
                                    if (configMapList == null) {
                                        return Single.error(
                                            new ResourceNotFoundException(
                                                String.format("Unable to retrieve list of configmaps from namespace %s", namespace)
                                            )
                                        );
                                    } else {
                                        return Single.just(configMapList);
                                    }
                                }
                            );
                    }
                }
            );
    }

    @Override
    public <T> Maybe<T> get(String location, Class<T> type) {
        KubernetesResource resource = new KubernetesResource(location);

        LOGGER.debug("Retrieve [{}] in namespace [{}]", resource.type.value, resource.namespace);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setURI(generateRequestUri(resource));
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
                                String.format(
                                    "Unable to retrieve %s %s from namespace %s. error code %d",
                                    resource.type.value,
                                    resource.name,
                                    resource.namespace,
                                    response.statusCode()
                                )
                            )
                        );
                    } else {
                        return response
                            .rxBody()
                            .toMaybe()
                            .flatMap(
                                buffer -> {
                                    JsonObject item = buffer.toJsonObject();
                                    T data = null;
                                    if (resource.key == null) {
                                        data = item.mapTo(type);
                                    } else {
                                        if (!type.equals(String.class)) {
                                            return Maybe.error(
                                                new RuntimeException(
                                                    "Only String.class is supported when getting a specific key inside a ConfigMap or Secret ..."
                                                )
                                            );
                                        }

                                        String kind = item.getString("kind");
                                        if (kind.equals("ConfigMap")) {
                                            data = (T) item.mapTo(ConfigMap.class).getData().get(resource.key);
                                        } else if (kind.equals("Secret")) {
                                            data = (T) item.mapTo(Secret.class).getData().get(resource.key);
                                        }
                                    }

                                    if (data == null) {
                                        LOGGER.error(
                                            "Unable to retrieve {} {} from namespace {}",
                                            resource.type.value,
                                            resource.name,
                                            resource.namespace
                                        );
                                        return Maybe.empty();
                                    } else {
                                        return Maybe.just(data);
                                    }
                                }
                            );
                    }
                }
            );
    }

    /** @noinspection unchecked*/
    @Override
    public <T extends Event<?>> Flowable<T> watch(String location, Class<T> type) {
        KubernetesResource resource = new KubernetesResource(location);

        String fieldSelector = resource.name == null ? "" : String.format("metadata.name=%s", resource.name);
        String watchKey = location + "#" + fieldSelector;

        final Watch<T> watch = watchMap.computeIfAbsent(
            watchKey,
            s -> {
                final Watch<T> w = watchEvents(resource, fieldSelector, watchKey, type);
                w.setEvents(w.events.doFinally(() -> watchMap.remove(watchKey)));
                return w;
            }
        );

        return watch.events;
    }

    @Override
    public Future<Void> stop() {
        final Promise<Void> promise = Promise.promise();

        httpClient.close(
            event -> {
                if (event.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(event.cause());
                }
            }
        );

        watchMap.clear();

        return promise.future();
    }

    private <T extends Event<?>> Watch<T> watchEvents(KubernetesResource resource, String fieldSelector, String watchKey, Class<T> type) {
        LOGGER.info("Start watching namespace [{}] with fieldSelector [{}]", resource.namespace, fieldSelector);

        final Watch<T> watch = new Watch<>(watchKey);
        final WebSocketConnectOptions webSocketConnectOptions = buildWebSocketConnectOptions(resource, fieldSelector, type);

        final Flowable<T> events = Flowable
            .<T>create(
                emitter -> {
                    httpClient()
                        .rxWebSocket(webSocketConnectOptions)
                        .flatMapPublisher(
                            websocket ->
                                websocket
                                    .toFlowable()
                                    .map(response -> response.toJsonObject().mapTo(type))
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
            .doOnError(
                throwable ->
                    LOGGER.error(
                        "An error occurred watching {} {} in namespace {}",
                        resource.type,
                        resource.name,
                        resource.namespace,
                        throwable
                    )
            )
            .publish()
            .refCount();

        watch.setEvents(events);

        return watch;
    }

    private <T extends Event<?>> WebSocketConnectOptions buildWebSocketConnectOptions(
        KubernetesResource resource,
        String fieldSelector,
        Class<T> type
    ) {
        return new WebSocketConnectOptions()
            .setURI(watcherUrlPath(resource.namespace, fieldSelector, type))
            .setHost(kubeConfig().getApiServerHost())
            .setPort(kubeConfig().getApiServerPort())
            .setSsl(kubeConfig().useSSL())
            .addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kubeConfig().getAccessToken());
    }

    private String generateRequestUri(KubernetesResource resource) {
        switch (resource.type) {
            case CONFIGMAPS:
                return String.format("/api/v1/namespaces/%s/configmaps/%s", resource.namespace, resource.name);
            case SECRETS:
                return String.format("/api/v1/namespaces/%s/secrets/%s", resource.namespace, resource.name);
            default:
                return null;
        }
    }

    private <T> String watcherUrlPath(String namespace, String fieldSelector, Class<T> type) {
        if (type == null) {
            return null;
        }

        if (type.equals(ConfigMapEvent.class)) {
            return (
                "/api/v1/namespaces/" +
                namespace +
                "/configmaps?" +
                "watch=true" +
                "&" +
                "allowWatchBookmarks=true" +
                "&" +
                "fieldSelector=" +
                fieldSelector
            );
        } else if (type.equals(SecretEvent.class)) {
            return (
                "/api/v1/namespaces/" +
                namespace +
                "/secrets?" +
                "watch=true" +
                "&" +
                "allowWatchBookmarks=true" +
                "&" +
                "fieldSelector=" +
                fieldSelector
            );
        }

        return null;
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

    private static class Watch<T extends Event<?>> {

        private final String key;
        private long timerId;
        private Flowable<T> events;

        public Watch(String key) {
            this.key = key;
        }

        public Flowable<T> getEvents() {
            return events;
        }

        public void setEvents(Flowable<T> events) {
            this.events = events;
        }

        public String getKey() {
            return key;
        }
    }

    public static class KubernetesResource {

        private String namespace;
        private KubernetesResourceType type;
        private String name;
        private String key;

        /**
         * @param location A location is where your Kubernetes resource is located. It is also possible
         * to ask for a specific key inside a configmap or secret. Example:
         * /my_namespace/configmaps/my_configmap
         * /my_namespace/secretes/my_secret/tls.key
         */
        public KubernetesResource(String location) {
            String[] properties = location.substring(1).split("/"); // eliminate the initial /

            if (properties.length < 2 || hasEmptyValues(properties)) {
                throw new RuntimeException("Wrong location. A correct format looks like this \"/{namespace}/configmaps/{configmap-name}\"");
            }

            String resourceKey = properties.length == 4 ? properties[3] : null;
            KubernetesResourceType resourceType = null;
            for (KubernetesResourceType rt : KubernetesResourceType.values()) {
                if (rt.value().equals(properties[1])) {
                    resourceType = rt;
                }
            }

            String resourceName = properties.length > 2 ? properties[2] : null;

            this.setNamespace(properties[0]);
            this.setType(resourceType);
            this.setName(resourceName);
            this.setKey(resourceKey);
        }

        private boolean hasEmptyValues(String[] properties) {
            for (String property : properties) {
                if (!StringUtils.hasText(property)) {
                    return true;
                }
            }
            return false;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            Assert.notNull(namespace, "Namespace can not be null");
            this.namespace = namespace;
        }

        public KubernetesResourceType getType() {
            return type;
        }

        public void setType(KubernetesResourceType type) {
            Assert.notNull(type, "Resource type can not be null");
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public enum KubernetesResourceType {
        SECRETS("secrets"),
        CONFIGMAPS("configmaps");

        private final String value;

        KubernetesResourceType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}
