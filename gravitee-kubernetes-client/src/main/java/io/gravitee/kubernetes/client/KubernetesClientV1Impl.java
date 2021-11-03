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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
    private final Map<String, Watch> watchMap = new ConcurrentHashMap<>();
    private final Map<String, Flowable<? extends Event<?>>> flowableMap = new ConcurrentHashMap<>();

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
        KubernetesResource resource = new KubernetesResource(location);

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
                                        if (!type.equals(String.class)) {
                                            return Maybe.error(
                                                new RuntimeException(
                                                    "Only String.class is suppoerted when getting a specific key inside a ConfigMap or Secret ..."
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

    @Override
    public <T extends Event<?>> Flowable<T> watch(String location, Class<T> type) {
        KubernetesResource resource = new KubernetesResource(location);

        String fieldSelector = resource.name == null ? "" : String.format("metadata.name=%s", resource.name);
        Watch watch = new Watch();
        watchMap.putIfAbsent(watch.uid, watch);

        String flowableKey = fieldSelector.equals("") ? resource.namespace : resource.name;
        if (flowableMap.containsKey(flowableKey)) {
            return (Flowable<T>) flowableMap.get(flowableKey);
        }

        LOGGER.info("Start watching namespace [{}] with fieldSelector [{}]", resource.namespace, fieldSelector);
        Flowable<T> flowable = Flowable
            .<T>create(emitter -> fetchEvents(emitter, resource, fieldSelector, watch.uid, type), BackpressureStrategy.BUFFER)
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
            .doFinally(() -> flowableMap.remove(flowableKey))
            .publish()
            .refCount();

        flowableMap.put(flowableKey, flowable);

        return flowable;
    }

    @Override
    public Future<Void> stop() {
        watchMap.forEach((k, v) -> v.stopped = true);
        return Future.succeededFuture();
    }

    private <T extends Event<?>> void fetchEvents(
        FlowableEmitter<T> emitter,
        KubernetesResource resource,
        String fieldSelector,
        String uid,
        Class<T> type
    ) {
        WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions()
            .setURI(watcherUrlPath(resource.namespace, fieldSelector, type))
            .setHost(config.getApiServerHost())
            .setPort(config.getApiServerPort())
            .setSsl(config.useSSL())
            .addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());

        httpClient
            .rxWebSocket(webSocketConnectOptions)
            .flatMapObservable(
                websocket ->
                    websocket
                        .toObservable()
                        .doOnSubscribe(
                            disposable -> {
                                watchMap.get(uid).retries = 0;
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
                                    );
                            }
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
                        .skip(1) // each time you connect to the API server, you get an initial ADD event representing the current resource
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
                                }
                                emitter.onError(throwable);
                                if (watchMap.get(uid).retries < 5) {
                                    LOGGER.info(
                                        "An error occurred connecting to the Kubernetes API server, trying to reconnect in 5 seconds ..."
                                    );
                                    watchMap.get(uid).retries++;
                                    Thread.sleep(5 * 1000L);
                                    fetchEvents(emitter, resource, fieldSelector, uid, type);
                                } else {
                                    watchMap.get(uid).stopped = true;
                                }
                            }
                        )
                        .doOnComplete(
                            () -> {
                                if (!watchMap.get(uid).stopped) {
                                    fetchEvents(emitter, resource, fieldSelector, uid, type);
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

    private HttpClientOptions getHttpClientOptions() {
        PemTrustOptions trustOptions = new PemTrustOptions();
        if (config.getCaCertData() == null || config.getApiServerHost() == null || config.getApiServerPort() == 0) {
            LOGGER.error(
                "KubeConfig is not configured properly. If you are running locally make sure you already configured your kubeconfig"
            );
        }

        if (config.getCaCertData() != null) {
            trustOptions.addCertValue(Buffer.buffer(config.getCaCertData()));
        }

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
        private int retries;

        public Watch() {
            this.stopped = false;
            this.uid = UUID.random().toString();
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
