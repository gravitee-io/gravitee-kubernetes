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
package io.gravitee.gateway.services.kube.services.impl;

import static java.lang.String.format;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.exceptions.SecretAccessException;
import io.gravitee.gateway.services.kube.exceptions.SecretNotFoundException;
import io.gravitee.gateway.services.kube.services.KubernetesService;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class KubernetesServiceImpl implements KubernetesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesServiceImpl.class);

    protected KubernetesClient client;

    public KubernetesServiceImpl(@Autowired @Lazy KubernetesClient client) {
        this.client = client;
    }

    @Override
    public String retrieveSecret(String namespace, String secretName, String secretKey)
        throws SecretNotFoundException, SecretAccessException {
        LOGGER.debug("resolving secret {}.{} in namespce {}", secretName, secretKey, namespace);

        try {
            Secret k8sSecret = client.secrets().inNamespace(namespace).withName(secretName).get();
            if (k8sSecret == null) {
                throw new SecretNotFoundException(format("Secret '%s' not found in '%s' namespace.", secretName, namespace));
            }

            if (k8sSecret.getData() == null || !k8sSecret.getData().containsKey(secretKey)) {
                throw new SecretNotFoundException(
                    format("Key '%s' not found in secret '%s' (namespace=%s).", secretKey, secretName, namespace)
                );
            }

            String b64Secret = k8sSecret.getData().get(secretKey);
            return new String(Base64.getDecoder().decode(b64Secret));
        } catch (KubernetesClientException e) {
            throw new SecretAccessException(format("Unable to access Secret '%s' (namespace=%s).", secretName, namespace));
        }
    }

    @Override
    public Map<String, Object> resolveSecret(WatchActionContext<?> context, String namespace, Map<String, Object> config) {
        if (config != null) {
            return config
                .entrySet()
                .stream()
                .map(
                    entry -> {
                        // first transformation will replace secret reference by the secret value
                        if (entry.getKey().equalsIgnoreCase("valueFrom")) {
                            final Map secretRef = (Map) entry.getValue();
                            if (secretRef.size() == 1 && secretRef.containsKey("secretKeyRef")) {
                                Optional<String> secretNamespace = Optional
                                    .ofNullable(((Map) secretRef.get("secretKeyRef")).get("namespace"))
                                    .map(String::valueOf);
                                final String secretName = String.valueOf(((Map) secretRef.get("secretKeyRef")).get("name"));
                                final String secretKey = String.valueOf(((Map) secretRef.get("secretKeyRef")).get("key"));
                                try {
                                    entry.setValue(retrieveSecret(secretNamespace.orElse(namespace), secretName, secretKey));
                                } catch (SecretAccessException | SecretNotFoundException e) {
                                    throw new PipelineException(
                                        context,
                                        format("Unable to read key '%s' in secret '%s'", secretKey, secretName)
                                    );
                                }
                            }
                        } else if (entry.getValue() instanceof Map) {
                            entry.setValue(resolveSecret(context, namespace, (Map<String, Object>) entry.getValue()));
                        }
                        return entry;
                    }
                )
                .map(
                    entry -> {
                        // second transformation flatten the config map to remove valueFrom level
                        if (entry.getValue() instanceof Map) {
                            if (((Map<?, ?>) entry.getValue()).size() == 1) {
                                final Map.Entry<?, ?> next = ((Map<?, ?>) entry.getValue()).entrySet().iterator().next();
                                if (next.getKey().equals("valueFrom")) {
                                    entry.setValue(next.getValue());
                                }
                            }
                        }
                        return entry;
                    }
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return Collections.emptyMap();
    }
}
