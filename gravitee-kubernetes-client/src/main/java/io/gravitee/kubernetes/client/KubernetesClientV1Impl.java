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
import io.reactivex.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.consul.Watch;
import io.vertx.reactivex.ext.web.client.WebClient;
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

    private final Vertx vertx;
    private final WebClient client;
    private final HttpClient httpClient;
    private final KubernetesConfig config;
    private Map<String, Watch> watchMap = new HashMap<>();

    @Autowired
    public KubernetesClientV1Impl(Vertx vertx, KubernetesConfig kubernetesConfig) {
        this.vertx = vertx;
        this.config = kubernetesConfig;
        this.client = WebClient.create(vertx, getHttpClientOptions());
        this.httpClient = vertx.createHttpClient(getHttpClientOptions());
    }

    @Override
    public Single<SecretList> secretList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

        LOGGER.debug("Retrieve list of secrets in namespace [{}]", namespace);
        return client
            .get(String.format("/api/v1/namespaces/%s/secrets", namespace))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getAccessToken())
            .rxSend()
            .map(
                response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException(
                            String.format(
                                "Unable to retrieve list of secrets from namespace %s. error code %d",
                                namespace,
                                response.statusCode()
                            )
                        );
                    } else {
                        SecretList secretList = response.bodyAsJsonObject().mapTo(SecretList.class);
                        if (secretList == null) {
                            throw new ResourceNotFoundException(
                                String.format("Unable to retrieve list of secrets from namespace %s", namespace)
                            );
                        } else {
                            return secretList;
                        }
                    }
                }
            );
    }

    @Override
    public Single<ConfigMapList> configMapList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

        LOGGER.debug("Retrieve list of configmaps in namespace [{}]", namespace);
        return client
            .get(String.format("/api/v1/namespaces/%s/configmaps", namespace))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getAccessToken())
            .rxSend()
            .map(
                response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException(
                            String.format(
                                "Unable to retrieve list of configmaps from namespace %s. error code %d",
                                namespace,
                                response.statusCode()
                            )
                        );
                    } else {
                        ConfigMapList configMapList = response.bodyAsJsonObject().mapTo(ConfigMapList.class);
                        if (configMapList == null) {
                            throw new ResourceNotFoundException(
                                String.format("Unable to retrieve list of configmaps from namespace %s", namespace)
                            );
                        } else {
                            return configMapList;
                        }
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

        return client
            .get(generateRequestUri(resource))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getAccessToken())
            .rxSend()
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
                        T data = null;
                        if (resource.key == null) {
                            data = response.bodyAsJsonObject().mapTo(type);
                        } else {
                            Object item = response.bodyAsJsonObject().mapTo(type);
                            if (item instanceof ConfigMap) {
                                data = (T) ((ConfigMap) item).getData().get(resource.key);
                            } else if (item instanceof Secret) {
                                data = (T) ((Secret) item).getData().get(resource.key);
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
                }
            );
    }

    @Override
    public <T extends Event> Flowable<T> watch(String location, Class<T> type) {
        KubeResource resource = parseLocation(location);
        if (resource == null) {
            return Flowable.empty();
        }

        String fieldSelector = resource.name == null ? "" : String.format("metadata.name=%s", resource.name);
        Watch watch = watchMap.computeIfAbsent(location, Watch::new);

        if (watchMap.get(watch.location).stopped) {
            if (watchMap.get(watch.location).timerId != 0) {
                LOGGER.info("Cancel timer for Kubernetes resource watcher {}.", watch.location);
                vertx.cancelTimer(watchMap.get(watch.location).timerId);
                watchMap.remove(watch.location);
            }
            return Flowable.empty();
        }

        LOGGER.info("Start watching namespace [{}] with fieldSelector [{}]", resource.namespace, fieldSelector);
        return retrieveLastResourceVersion(resource.namespace, type)
            .flatMapObservable(lrv -> fetchEvents(resource, lrv, fieldSelector, watch.location, type))
            .doOnSubscribe(
                disposable ->
                    vertx.setPeriodic(
                        config.getWebsocketTimeout(),
                        l -> {
                            watchMap.get(watch.location).timerId = l;
                            watch(location, type);
                        }
                    )
            )
            .doOnError(
                throwable -> {
                    LOGGER.error("Unable to get the last resource version", throwable);
                    watchMap.get(watch.location).stopped = true;
                }
            )
            .toFlowable(BackpressureStrategy.DROP);
    }

    @Override
    public Future<Void> stop(String location) {
        Assert.notNull(location, "Location can not be null");

        Promise<Void> promise = Promise.promise();
        if (watchMap.containsKey(location)) {
            Watch watch = watchMap.get(location);
            watch.stopped = true;
            watchMap.put(location, watch);
            promise.complete();
        } else {
            promise.fail(String.format("No watcher is found for %s", location));
        }

        return promise.future();
    }

    @Override
    public Future<Void> stopAll() {
        watchMap.forEach((k, v) -> v.stopped = true);
        return Future.succeededFuture();
    }

    private <T extends Event> Observable<T> fetchEvents(
        KubeResource resource,
        String lastResourceVersion,
        String fieldSelector,
        String location,
        Class<T> type
    ) {
        WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
        webSocketConnectOptions.setURI(watcherUrlPath(resource.namespace, lastResourceVersion, fieldSelector, type));
        webSocketConnectOptions.setHost(config.getApiServerHost());
        webSocketConnectOptions.setPort(config.getApiServerPort());
        webSocketConnectOptions.setSsl(true);
        webSocketConnectOptions.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        webSocketConnectOptions.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getAccessToken());

        return httpClient
            .rxWebSocket(webSocketConnectOptions)
            .flatMapObservable(
                websocket ->
                    websocket
                        .toObservable()
                        .flatMap(
                            response -> {
                                if (watchMap.get(location).stopped) {
                                    LOGGER.info("Stop Kubernetes resource watcher {}.", location);
                                    websocket.close();
                                    return Observable.empty();
                                }

                                T item = response.toJsonObject().mapTo(type);
                                // TODO - Kamiel - 12/10/2021: to be replaced with proper operation
                                if (
                                    item.getType().equals(KubernetesEventType.MODIFIED.name()) ||
                                    item.getType().equals(KubernetesEventType.ADDED.name())
                                ) {
                                    JsonObject json = response.toJsonObject();
                                    JsonObject object = json.getJsonObject("object");
                                    JsonObject metadata = object.getJsonObject("metadata");
                                    String name = metadata.getString("name");

                                    if (item instanceof ConfigMapEvent) {
                                        get(String.format("kube://%s/configmap/%s", resource.namespace, name), ConfigMap.class)
                                            .subscribe(((ConfigMapEvent) item)::setData, Throwable::printStackTrace);
                                    } else if (item instanceof SecretEvent) {
                                        get(String.format("kube://%s/secret/%s", resource.namespace, name), Secret.class)
                                            .subscribe(((SecretEvent) item)::setData, Throwable::printStackTrace);
                                    }
                                }

                                return Observable.just(item);
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

        String resourceName = properties.length == 3 ? properties[2] : null;

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

    public <T extends Event> Single<String> retrieveLastResourceVersion(String namespace, Class<T> type) {
        if (type.equals(ConfigMapEvent.class)) {
            return configMapList(namespace).map(configMapList -> configMapList.getMetadata().getResourceVersion());
        } else if (type.equals(SecretEvent.class)) {
            return secretList(namespace).map(secretList -> secretList.getMetadata().getResourceVersion());
        }

        return Single.error(new RuntimeException("Unable to determine the resource type " + type));
    }

    public <T> String watcherUrlPath(String namespace, String lastResourceVersion, String fieldSelector, Class<T> type) {
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

    private WebClientOptions getHttpClientOptions() {
        PemTrustOptions trustOptions = new PemTrustOptions();
        trustOptions.addCertValue(Buffer.buffer(config.getCaCertData()));

        return new WebClientOptions()
            .setTrustOptions(trustOptions)
            .setVerifyHost(config.verifyHost())
            .setTrustAll(!config.verifyHost())
            .setDefaultHost(config.getApiServerHost())
            .setDefaultPort(config.getApiServerPort())
            .setSsl(config.useSSL());
    }

    private static class Watch {

        private boolean stopped;
        private final String location;
        private long timerId;

        public Watch(String location) {
            this.stopped = false;
            this.location = location;
        }
    }
}
