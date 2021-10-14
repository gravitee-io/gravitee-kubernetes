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
import io.gravitee.common.utils.UUID;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.exception.ResourceNotFoundException;
import io.gravitee.kubernetes.client.model.v1.*;
import io.reactivex.*;
import io.vertx.core.Future;
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
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@Component
public class KubernetesClientV1Impl implements KubernetesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientV1Impl.class);
    private static final long PING_HANDLER_DELAY = 5000L;
    private final Vertx vertx;
    private final HttpClient httpClient;
    private final KubernetesConfig config;
    private final Map<String, Watch> watchMap = new HashMap<>();

    @Autowired
    public KubernetesClientV1Impl(Vertx vertx, KubernetesConfig kubernetesConfig) {
        this.vertx = vertx;
        this.config = kubernetesConfig;
        this.httpClient = vertx.createHttpClient(getHttpClientOptions());
    }

    @Override
    public Single<SecretList> secretList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

        LOGGER.debug("Retrieve list of secrets in namespace [{}]", namespace);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setURI(String.format("/api/v1/namespaces/%s/secrets", namespace));
        requestOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());
        return httpClient
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
        requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());
        return httpClient
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
        KubeResource resource = parseLocation(location);
        if (resource == null) {
            return Maybe.empty();
        }

        LOGGER.debug("Retrieve [{}] in namespace [{}]", resource.type.value, resource.namespace);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMethod(HttpMethod.GET);
        requestOptions.setURI(generateRequestUri(resource));
        requestOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());
        return httpClient
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
                                        String kind = item.getString("kind");
                                        if (kind != null) {
                                            if (kind.equalsIgnoreCase(ResourceType.CONFIGMAP.value)) {
                                                data = (T) item.mapTo(ConfigMap.class).getData().get(resource.key);
                                            } else if (kind.equalsIgnoreCase(ResourceType.SECRET.value())) {
                                                data = (T) item.mapTo(Secret.class).getData().get(resource.key);
                                            }
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

    @Override
    public <T extends Event<?>> Flowable<T> watch(String location, Class<T> type) {
        KubeResource resource = parseLocation(location);
        if (resource == null) {
            return Flowable.empty();
        }

        String fieldSelector = resource.name == null ? "" : String.format("metadata.name=%s", resource.name);
        Watch watch = new Watch();
        watchMap.putIfAbsent(watch.uid, watch);

        LOGGER.info("Start watching namespace [{}] with fieldSelector [{}]", resource.namespace, fieldSelector);
        return Flowable
            .<T>create(emitter -> eventEmitter(emitter, resource, fieldSelector, watch.uid, type), BackpressureStrategy.BUFFER)
            .doOnError(
                throwable ->
                    LOGGER.error(
                        "An error occurred watching {} {} in namespace {}",
                        resource.type,
                        resource.name,
                        resource.namespace,
                        throwable
                    )
            );
    }

    @Override
    public Future<Void> stop() {
        watchMap.forEach((k, v) -> v.stopped = true);
        return Future.succeededFuture();
    }

    private <T extends Event<?>> void eventEmitter(
        FlowableEmitter<T> emitter,
        KubeResource resource,
        String fieldSelector,
        String uid,
        Class<T> type
    ) {
        retrieveLastResourceVersion(resource.namespace, type)
            .doOnSuccess(lrv -> fetchEvents(emitter, resource, lrv, fieldSelector, uid, type))
            .doOnError(emitter::onError)
            .subscribe();
    }

    private <T extends Event<?>> void fetchEvents(
        FlowableEmitter<T> emitter,
        KubeResource resource,
        String lastResourceVersion,
        String fieldSelector,
        String uid,
        Class<T> type
    ) {
        WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
        webSocketConnectOptions.setURI(watcherUrlPath(resource.namespace, lastResourceVersion, fieldSelector, type));
        webSocketConnectOptions.setHost(config.getApiServerHost());
        webSocketConnectOptions.setPort(config.getApiServerPort());
        webSocketConnectOptions.setSsl(true);
        webSocketConnectOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        webSocketConnectOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());

        httpClient
            .rxWebSocket(webSocketConnectOptions)
            .flatMapObservable(
                websocket ->
                    websocket
                        .toObservable()
                        .doOnSubscribe(
                            disposable ->
                                watchMap.get(uid).timerId =
                                    vertx.setTimer(
                                        PING_HANDLER_DELAY,
                                        aLong -> {
                                            if (watchMap.get(uid).stopped) {
                                                websocket.close();
                                            } else {
                                                websocket.rxWritePing(io.vertx.reactivex.core.buffer.Buffer.buffer("ping"));
                                            }
                                        }
                                    )
                        )
                        .flatMap(
                            response -> {
                                if (watchMap.get(uid).stopped) {
                                    websocket.close();
                                    return Observable.empty();
                                }

                                return Observable.just(response.toJsonObject().mapTo(type));
                            }
                        )
                        .doOnNext(emitter::onNext)
                        .doOnError(
                            throwable -> {
                                LOGGER.error(
                                    "An error occurred watching {} {} in namespace {}, This watcher is stopped",
                                    resource.type,
                                    resource.name,
                                    resource.namespace
                                );
                                if (!websocket.isClosed()) {
                                    websocket.close();
                                    watchMap.get(uid).stopped = true;
                                }
                                emitter.onError(throwable);
                            }
                        )
                        .doOnComplete(
                            () -> {
                                if (!watchMap.get(uid).stopped) {
                                    eventEmitter(emitter, resource, fieldSelector, uid, type);
                                }
                            }
                        )
                        .doFinally(
                            () -> {
                                if (watchMap.get(uid).stopped) {
                                    vertx.cancelTimer(watchMap.get(uid).timerId);
                                    watchMap.remove(uid);
                                    emitter.onComplete();
                                }
                            }
                        )
            )
            .subscribe();
    }

    private KubeResource parseLocation(String location) {
        String[] properties = location.substring(7).split("/"); // eliminate initial kube://

        if (properties.length < 2) {
            LOGGER.error("Wrong location. A correct format looks like this \"kube://{namespace}/configmap/{configmap-name}\"");
            return null;
        }

        String key = properties.length == 4 ? properties[3] : null;
        ResourceType resourceType = null;
        for (ResourceType type : ResourceType.values()) {
            if (type.value().equals(properties[1])) {
                resourceType = type;
            }
        }

        String resourceName = properties.length > 2 ? properties[2] : null;

        return new KubeResource(properties[0], resourceType, resourceName, key);
    }

    private String generateRequestUri(KubeResource resource) {
        switch (resource.type) {
            case CONFIGMAP:
                return String.format("/api/v1/namespaces/%s/configmaps/%s", resource.namespace, resource.name);
            case SECRET:
                return String.format("/api/v1/namespaces/%s/secrets/%s", resource.namespace, resource.name);
            default:
                return null;
        }
    }

    private <T extends Event<?>> Single<String> retrieveLastResourceVersion(String namespace, Class<T> type) {
        if (type.equals(ConfigMapEvent.class)) {
            return configMapList(namespace).map(configMapList -> configMapList.getMetadata().getResourceVersion());
        } else if (type.equals(SecretEvent.class)) {
            return secretList(namespace).map(secretList -> secretList.getMetadata().getResourceVersion());
        }

        return Single.error(new RuntimeException("Unable to determine the resource type " + type));
    }

    private <T> String watcherUrlPath(String namespace, String lastResourceVersion, String fieldSelector, Class<T> type) {
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
                fieldSelector +
                "&" +
                "resourceVersion=" +
                lastResourceVersion
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
                fieldSelector +
                "&" +
                "resourceVersion=" +
                lastResourceVersion
            );
        }

        return null;
    }

    private HttpClientOptions getHttpClientOptions() {
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

    private static class Watch {

        private boolean stopped;
        private final String uid;
        private long timerId;

        public Watch() {
            this.stopped = false;
            this.uid = UUID.random().toString();
        }
    }

    private static class KubeResource {

        private final String namespace;
        private final ResourceType type;
        private final String name;
        private final String key;

        private KubeResource(String namespace, ResourceType type, String name, String key) {
            Assert.notNull(namespace, "Namespace can not be null");
            Assert.notNull(type, "Resource type can not be null");

            this.namespace = namespace;
            this.type = type;
            this.name = name;
            this.key = key;
        }
    }

    private enum ResourceType {
        SECRET("secret"),
        CONFIGMAP("configmap");

        private final String value;

        ResourceType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}
