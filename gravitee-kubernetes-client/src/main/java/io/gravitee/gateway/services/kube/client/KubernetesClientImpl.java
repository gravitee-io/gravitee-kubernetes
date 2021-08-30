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
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@Component
public class KubernetesClientImpl implements KubernetesClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesClientImpl.class);

    private final WebClient client;
    private final KubernetesConfig config;

    @Autowired
    public KubernetesClientImpl(Vertx vertx, KubernetesConfig kubernetesConfig) {
        this.client = WebClient.create(vertx, getHttpClientOptions());
        this.config = kubernetesConfig;
    }

    @Override
    public Single<SecretList> secretList(String namespace) {
        return client
            .get(String.format("/api/v1/namespaces/%s/secrets", namespace))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .putHeader(HttpHeaders.ACCEPT, "Bearer " + config.getServiceAccountToken())
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
        return client
            .get(String.format("/api/v1/secret/%s/%s", namespace, secretName))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getServiceAccountToken())
            .rxSend()
            .toMaybe()
            .map(
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
                            return null;
                        } else {
                            return secret;
                        }
                    }
                }
            );
    }

    @Override
    public Single<ConfigMapList> configMapList(String namespace) {
        return client
            .get(String.format("/api/v1/namespaces/%s/configmaps", namespace))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getServiceAccountToken())
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
        return client
            .get(String.format("/api/v1/namespaces/%s/configmaps/%s", namespace, configmapName))
            .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .bearerTokenAuthentication(config.getServiceAccountToken())
            .rxSend()
            .toMaybe()
            .map(
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
                            return null;
                        } else {
                            return configMap;
                        }
                    }
                }
            );
    }

    private WebClientOptions getHttpClientOptions() {
        JksOptions jksOptions = new JksOptions().setValue(Buffer.buffer(config.getCaTrustStore())).setPassword("");

        return new WebClientOptions()
            .setKeyStoreOptions(jksOptions)
            .setVerifyHost(false)
            .setTrustAll(true)
            .setDefaultHost(config.getApiServerHost())
            .setDefaultPort(config.getApiServerPort())
            .setSsl(true);
    }
}
