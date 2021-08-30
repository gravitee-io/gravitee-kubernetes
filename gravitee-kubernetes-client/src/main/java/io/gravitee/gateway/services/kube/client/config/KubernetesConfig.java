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
package io.gravitee.gateway.services.kube.client.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public class KubernetesConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesConfig.class);

    public static final String KUBERNETES_SERVICE_HOST_PROPERTY = "KUBERNETES_SERVICE_HOST";
    public static final String KUBERNETES_SERVICE_PORT_HTTPS_PROPERTY = "KUBERNETES_SERVICE_PORT_HTTPS";
    public static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    public static final String KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    public static final Long DEFAULT_WEBSOCKET_TIMEOUT = 5 * 1000L;

    private String apiServerHost;
    private int apiServerPort;
    private String caCertData;
    private String namespace;
    private String serviceAccountToken;
    private long websocketTimeout = DEFAULT_WEBSOCKET_TIMEOUT;

    public KubernetesConfig() {
        loadApiServerInfo();
        loadKubernetesCaFile();
        loadServiceAccountToken();
    }

    /**
     * Find the Kubernetes API Server HOST, PORT from within the pod
     */
    private void loadApiServerInfo() {
        LOGGER.debug("Trying to configure client from service account...");
        String host = getSystemPropertyOrEnvVar(KUBERNETES_SERVICE_HOST_PROPERTY, null);
        String port = getSystemPropertyOrEnvVar(KUBERNETES_SERVICE_PORT_HTTPS_PROPERTY, null);

        if (!StringUtils.isEmpty(host) && !StringUtils.isEmpty(port)) {
            LOGGER.debug("Found API Server host and port: {}:{}", host, port);

            setApiServerHost(host);
            setApiServerPort(Integer.parseInt(port));
        } else {
            LOGGER.error("Unable to resolve the API Server URL");
        }
    }

    /**
     * Load the Kubernetes CA from within the pod
     */
    private void loadKubernetesCaFile() {
        try {
            boolean serviceAccountCaCertExists = Files.isRegularFile(new File(KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH).toPath());
            if (serviceAccountCaCertExists) {
                LOGGER.debug("Found service account ca cert at: [{}]", KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH);
                this.setCaCertData(new String(Files.readAllBytes(new File(KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH).toPath())));
            } else {
                LOGGER.error("Did not find service account ca cert at: [{}]", KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH);
            }
        } catch (IOException e) {
            // No CA file available...
            LOGGER.error("Error reading Kubernetes CA file from: [{}].", KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH, e);
        }
    }

    /**
     * Load the Kubernetes Service account from within the pod
     */
    private void loadServiceAccountToken() {
        try {
            String serviceTokenCandidate = new String(Files.readAllBytes(new File(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH).toPath()));
            LOGGER.debug("Found service account token at: [" + KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH + "].");

            this.setServiceAccountToken(serviceTokenCandidate);
        } catch (IOException e) {
            // No service account token available...
            LOGGER.error("Error reading service account token from: [{}].", KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH, e);
        }
    }

    private <T> String getSystemPropertyOrEnvVar(String propertyName, T defaultValue) {
        String value = System.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        return System.getenv().getOrDefault(propertyName, (defaultValue != null ? defaultValue.toString() : null));
    }

    // Property methods
    public String getApiServerHost() {
        return apiServerHost;
    }

    public void setApiServerHost(String apiServerHost) {
        this.apiServerHost = apiServerHost;
    }

    public int getApiServerPort() {
        return apiServerPort;
    }

    public void setApiServerPort(int apiServerPort) {
        this.apiServerPort = apiServerPort;
    }

    public String getCaCertData() {
        return caCertData;
    }

    public void setCaCertData(String caCertData) {
        this.caCertData = caCertData;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServiceAccountToken() {
        return serviceAccountToken;
    }

    public void setServiceAccountToken(String serviceAccountToken) {
        this.serviceAccountToken = serviceAccountToken;
    }

    public long getWebsocketTimeout() {
        return websocketTimeout;
    }

    public void setWebsocketTimeout(long websocketTimeout) {
        this.websocketTimeout = websocketTimeout;
    }
}
