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
import io.gravitee.gateway.services.kube.client.exception.ResourceNotFoundException;
import io.gravitee.gateway.services.kube.client.model.v1.ConfigMap;
import io.gravitee.gateway.services.kube.client.model.v1.ConfigMapList;
import io.gravitee.gateway.services.kube.client.model.v1.Secret;
import io.gravitee.gateway.services.kube.client.model.v1.SecretList;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
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

    private final WebClient client;
    private final KubernetesConfig config;

    @Autowired
    public KubernetesClientV1Impl(Vertx vertx, KubernetesConfig kubernetesConfig) {
        this.config = kubernetesConfig;
        this.client = WebClient.create(vertx, getHttpClientOptions());
    }

    @Override
    public Single<SecretList> secretList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

        return client
            .get(String.format("/api/v1/namespaces/%s/secrets", namespace))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .putHeader(HttpHeaders.ACCEPT, "Bearer " + config.getAccessToken())
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
    public Maybe<Secret> secret(String namespace, String secretName) {
        Assert.notNull(namespace, "Namespace can't be null");
        Assert.notNull(secretName, "Resource name can't be null");

        return client
            .get(String.format("/api/v1/namespaces/%s/secrets/%s", namespace, secretName))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getAccessToken())
            .rxSend()
            .toMaybe()
            .flatMap(
                response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException(
                            String.format(
                                "Unable to retrieve secret %s from namespace %s. error code %d",
                                secretName,
                                namespace,
                                response.statusCode()
                            )
                        );
                    } else {
                        Secret secret = response.bodyAsJsonObject().mapTo(Secret.class);
                        if (secret == null) {
                            LOGGER.error("Unable to retrieve secret {} from namespace {}", secretName, namespace);
                            return Maybe.empty();
                        } else {
                            return Maybe.just(secret);
                        }
                    }
                }
            );
    }

    @Override
    public Single<ConfigMapList> configMapList(String namespace) {
        Assert.notNull(namespace, "Namespace can't be null");

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
    public Maybe<ConfigMap> configMap(String namespace, String configmapName) {
        Assert.notNull(namespace, "Namespace can't be null");
        Assert.notNull(configmapName, "Resource name can't be null");

        return client
            .get(String.format("/api/v1/namespaces/%s/configmaps/%s", namespace, configmapName))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getAccessToken())
            .rxSend()
            .toMaybe()
            .flatMap(
                response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException(
                            String.format(
                                "Unable to retrieve configmap %s from namespace %s. error code %d",
                                configmapName,
                                namespace,
                                response.statusCode()
                            )
                        );
                    } else {
                        ConfigMap configMap = response.bodyAsJsonObject().mapTo(ConfigMap.class);
                        if (configMap == null) {
                            LOGGER.error("Unable to retrieve configmap {} from namespace {}", configmapName, namespace);
                            return Maybe.empty();
                        } else {
                            return Maybe.just(configMap);
                        }
                    }
                }
            );
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
}
