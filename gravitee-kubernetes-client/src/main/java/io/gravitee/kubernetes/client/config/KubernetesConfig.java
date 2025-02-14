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
package io.gravitee.kubernetes.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.gravitee.kubernetes.client.model.config.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@Setter
@Getter
public class KubernetesConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesConfig.class);

    public static final String KUBERNETES_SERVICE_HOST_PROPERTY = "KUBERNETES_SERVICE_HOST";
    public static final String KUBERNETES_SERVICE_PORT_HTTPS_PROPERTY = "KUBERNETES_SERVICE_PORT_HTTPS";
    public static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH = "KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH";
    public static final String KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH = "KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH";
    public static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN_PROJECTION_ENABLED = "KUBERNETES_SERVICE_ACCOUNT_TOKEN_PROJECTION_ENABLED";
    public static final String KUBERNETES_CURRENT_NAMESPACE_PATH = "KUBERNETES_CURRENT_NAMESPACE";
    public static final String KUBERNETES_KUBECONFIG_FILE = "kubeconfig";
    static final Long DEFAULT_WEBSOCKET_TIMEOUT = 5 * 60 * 1000L;
    static final Integer DEFAULT_API_TIMEOUT = 5 * 60 * 1000;
    private String apiServerHost;
    private int apiServerPort;
    private String caCertData;

    @Getter(AccessLevel.NONE)
    private boolean verifyHost = true;

    @Getter(AccessLevel.NONE)
    private boolean useSSL = true;

    private String accessToken;
    private boolean accessTokenProjectionEnabled;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Instant accessTokenLastReload;

    private String currentNamespace;
    private long websocketTimeout = DEFAULT_WEBSOCKET_TIMEOUT;
    private int apiTimeout = DEFAULT_API_TIMEOUT;
    private String masterUrl;
    private String apiVersion = "v1";
    private String clientCertData;
    private String clientKeyData;
    private File file;
    private String username;
    private String password;

    private static KubernetesConfig instance;

    private KubernetesConfig() {
        // no op
    }

    public static synchronized KubernetesConfig getInstance() {
        if (instance == null) {
            instance = newInstance().initWithDefaults();
        }
        return instance;
    }

    public static KubernetesConfig newInstance() {
        return new KubernetesConfig();
    }

    public static KubernetesConfig newInstance(String kubeConfigLocation) {
        return newInstance().initWithConfigFile(kubeConfigLocation);
    }

    private KubernetesConfig initWithDefaults() {
        if (!(tryKubeConfig() || tryServiceAccount())) {
            LOGGER.error("Unable to configure Kubernetes Config. No KubeConfig or Service account is found");
        }
        return this;
    }

    private KubernetesConfig initWithConfigFile(String kubeConfigLocation) {
        if (!(tryKubeConfig(kubeConfigLocation) || tryServiceAccount())) {
            throw new IllegalArgumentException("Unable to configure Kubernetes Config. No KubeConfig or Service account is found");
        }
        return this;
    }

    boolean tryServiceAccount() {
        return loadApiServerInfo() && loadKubernetesCaFile() && loadServiceAccountToken();
    }

    /**
     * Find the Kubernetes API Server HOST, PORT from within the pod
     */
    private boolean loadApiServerInfo() {
        LOGGER.debug("Trying to configure client using service account...");
        String host = getSystemPropertyOrEnvVar(KUBERNETES_SERVICE_HOST_PROPERTY, null);
        String port = getSystemPropertyOrEnvVar(KUBERNETES_SERVICE_PORT_HTTPS_PROPERTY, null);

        if (host != null && !host.isEmpty() && port != null && !port.isEmpty()) {
            LOGGER.debug("Found API Server host and port: {}:{}", host, port);

            setApiServerHost(host);
            setApiServerPort(Integer.parseInt(port));
            setAccessTokenProjectionEnabled(
                Boolean.parseBoolean(getSystemPropertyOrEnvVar(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PROJECTION_ENABLED, "false"))
            );
            return true;
        } else {
            LOGGER.error("Unable to resolve the API Server URL");
            return false;
        }
    }

    /**
     * Load the Kubernetes CA from within the pod
     */
    private boolean loadKubernetesCaFile() {
        String caFilePath = getSystemPropertyOrEnvVar(
            KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH,
            "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"
        );
        try {
            boolean serviceAccountCaCertExists = Files.isRegularFile(new File(caFilePath).toPath());
            if (serviceAccountCaCertExists) {
                LOGGER.debug("Found service account ca cert at: [{}]", caFilePath);
                this.setCaCertData(new String(Files.readAllBytes(new File(caFilePath).toPath())));
                return true;
            } else {
                LOGGER.error("Did not find service account ca cert at: [{}]", caFilePath);
            }
        } catch (IOException e) {
            // No CA file available...
            LOGGER.error("Error reading Kubernetes CA file from: [{}].", caFilePath, e);
        }

        return false;
    }

    /**
     * Load the Kubernetes Service account from within the pod
     */
    public boolean loadServiceAccountToken() {
        return loadServiceAccountToken(null);
    }

    /**
     * Load the Kubernetes Service account with token from a file
     * @param tokenPath file containing token or null to use the default location
     */
    public boolean loadServiceAccountToken(String tokenPath) {
        String fallbackTokenPath = tokenPath != null ? tokenPath : "/var/run/secrets/kubernetes.io/serviceaccount/token";
        String tokenFilePath = getSystemPropertyOrEnvVar(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH, fallbackTokenPath);
        try {
            boolean serviceAccountAccessTokenExists = Files.isRegularFile(new File(tokenFilePath).toPath());
            if (serviceAccountAccessTokenExists) {
                LOGGER.debug("Found service account token at: [{}].", tokenFilePath);
                this.setAccessToken(Files.readString(Path.of(tokenFilePath)));
                this.accessTokenLastReload = Instant.now();
                return true;
            }
        } catch (IOException e) {
            // No service account token available...
            LOGGER.error("Error reading service account token from: [{}].", tokenFilePath, e);
        }

        return false;
    }

    private void loadCurrentNamespace() {
        String namespaceFilePath = getSystemPropertyOrEnvVar(
            KUBERNETES_CURRENT_NAMESPACE_PATH,
            "/var/run/secrets/kubernetes.io/serviceaccount/namespace"
        );
        try {
            boolean namespaceExists = Files.isRegularFile(new File(namespaceFilePath).toPath());
            if (namespaceExists) {
                String namespace = new String(Files.readAllBytes(new File(namespaceFilePath).toPath()));
                this.setCurrentNamespace(namespace);
                LOGGER.debug("Found the current namespace [{}] at: [{}].", namespace, namespaceFilePath);
            }
        } catch (IOException e) {
            // No service account token available...
            LOGGER.error("Unable to read the current namespace from file: [{}].", namespaceFilePath, e);
        }
    }

    public boolean tryKubeConfig() {
        return tryKubeConfig(null);
    }

    public boolean tryKubeConfig(String kubeConfigLocation) {
        LOGGER.debug("Trying to configure client from Kubernetes config...");
        File kubeConfigFile = new File(getKubeConfigFilename(kubeConfigLocation));
        if (!kubeConfigFile.isFile()) {
            LOGGER.debug("Did not find Kubernetes config at: [{}]. Ignoring.", kubeConfigFile.getPath());
            return false;
        }
        LOGGER.debug("Found Kubernetes config at: [{}].", kubeConfigFile.getPath());
        String kubeConfigContents = getKubeConfigContents(kubeConfigFile);
        if (kubeConfigContents == null) {
            return false;
        }
        this.file = new File(kubeConfigFile.getPath());
        return loadFromKubeConfig(kubeConfigContents);
    }

    private boolean loadFromKubeConfig(String kubeConfigContents) {
        try {
            Config config = parseConfigFromString(kubeConfigContents);
            Context currentContext = getContext(config);
            if (currentContext != null) {
                String namespace = currentContext.getNamespace();
                this.setCurrentNamespace(namespace != null && !namespace.isBlank() ? namespace : "default");
            }
            Cluster currentCluster = getCluster(config, currentContext);
            if (currentCluster != null) {
                this.setMasterUrl(currentCluster.getServer());
                loadCA(currentCluster);
                AuthInfo currentAuthInfo = getUserAuthInfo(config, currentContext);
                if (currentAuthInfo != null) {
                    loadClientCertificate(currentAuthInfo);
                    loadClientKey(currentAuthInfo);
                    this.setAccessToken(currentAuthInfo.getToken());
                    this.setUsername(currentAuthInfo.getUsername());
                    this.setPassword(currentAuthInfo.getPassword());
                }
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse the kube config.", e);
        }

        return false;
    }

    private void loadCA(Cluster currentCluster) throws IOException {
        String certificateAuthorityData = currentCluster.getCertificateAuthorityData();
        String certificateAuthorityPemFile = currentCluster.getCertificateAuthority();
        if (hasValue(certificateAuthorityData)) {
            this.setCaCertData(fromBase64(certificateAuthorityData));
        } else if (hasValue(certificateAuthorityPemFile)) {
            this.setCaCertData(Files.readString(Path.of(certificateAuthorityPemFile)));
        } else {
            throw new IllegalStateException("Cannot read CA data from file: %s".formatted(file));
        }
    }

    private void loadClientKey(AuthInfo currentAuthInfo) throws IOException {
        String clientPrivateKeyData = currentAuthInfo.getClientKeyData();
        String clientPrivateKeyPemFile = currentAuthInfo.getClientKey();
        if (hasValue(clientPrivateKeyData)) {
            this.setClientKeyData(fromBase64(clientPrivateKeyData));
        } else if (hasValue(clientPrivateKeyPemFile)) {
            this.setClientKeyData(Files.readString(Path.of(clientPrivateKeyPemFile)));
        }
    }

    private void loadClientCertificate(AuthInfo currentAuthInfo) throws IOException {
        String clientCertificateData = currentAuthInfo.getClientCertificateData();
        String clientCertificatePemFile = currentAuthInfo.getClientCertificate();
        if (hasValue(clientCertificateData)) {
            this.setClientCertData(fromBase64(clientCertificateData));
        } else if (hasValue(clientCertificatePemFile)) {
            this.setClientCertData(Files.readString(Path.of(clientCertificatePemFile)));
        }
    }

    private static String fromBase64(String certificateAuthorityData) {
        return new String(Base64.getDecoder().decode(certificateAuthorityData), StandardCharsets.UTF_8);
    }

    private <T> String getSystemPropertyOrEnvVar(String propertyName, T defaultValue) {
        String value = System.getProperty(propertyName);
        if (hasValue(value)) {
            return value;
        }

        return System.getenv().getOrDefault(propertyName, (defaultValue != null ? defaultValue.toString() : null));
    }

    private Config parseConfigFromString(String contents) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(contents, Config.class);
    }

    private NamedContext getCurrentContext(Config config) {
        String contextName = config.getCurrentContext();
        if (contextName != null) {
            List<NamedContext> contextList = config.getContexts();
            if (contextList != null) {
                for (NamedContext context : contextList) {
                    if (contextName.equals(context.getName())) {
                        return context;
                    }
                }
            }
        }
        return null;
    }

    private Context getContext(Config config) {
        NamedContext currentNamedContext = getCurrentContext(config);
        if (currentNamedContext != null) {
            return currentNamedContext.getContext();
        }

        return null;
    }

    private Cluster getCluster(Config config, Context context) {
        Cluster cluster = null;
        if (config != null && context != null) {
            String clusterName = context.getCluster();
            if (clusterName != null) {
                List<NamedCluster> clusters = config.getClusters();
                if (clusters != null) {
                    cluster =
                        clusters.stream().filter(c -> c.getName().equals(clusterName)).findAny().map(NamedCluster::getCluster).orElse(null);
                }
            }
        }
        return cluster;
    }

    private AuthInfo getUserAuthInfo(Config config, Context context) {
        AuthInfo authInfo = null;
        if (config != null && context != null) {
            String user = context.getUser();
            if (user != null) {
                List<NamedAuthInfo> users = config.getUsers();
                if (users != null) {
                    authInfo = users.stream().filter(u -> u.getName().equals(user)).findAny().map(NamedAuthInfo::getUser).orElse(null);
                }
            }
        }
        return authInfo;
    }

    private String getKubeConfigContents(File kubeConfigFile) {
        try (FileReader reader = new FileReader(kubeConfigFile); StringWriter writer = new StringWriter()) {
            char[] buffer = new char[8192];
            int len;
            for (;;) {
                len = reader.read(buffer);
                if (len > 0) {
                    writer.write(buffer, 0, len);
                } else {
                    writer.flush();
                    break;
                }
            }
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Could not load Kubernetes config file from {}", kubeConfigFile.getPath(), e);
            return null;
        }
    }

    private String getHomeDir() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.startsWith("win")) {
            String homeDir = getWinHomeDir();
            if (homeDir != null) return homeDir;
        }
        String home = System.getenv("HOME");
        if (hasValue(home)) {
            File f = new File(home);
            if (f.exists() && f.isDirectory()) {
                return home;
            }
        }

        //Fall back to user.home should never really get here
        return System.getProperty("user.home", ".");
    }

    private String getWinHomeDir() {
        String homeDrive = System.getenv("HOMEDRIVE");
        String homePath = System.getenv("HOMEPATH");
        if (homeDrive != null && !homeDrive.isEmpty() && homePath != null && !homePath.isEmpty()) {
            String homeDir = homeDrive + homePath;
            File f = new File(homeDir);
            if (f.exists() && f.isDirectory()) {
                return homeDir;
            }
        }
        String userProfile = System.getenv("USERPROFILE");
        if (hasValue(userProfile)) {
            File f = new File(userProfile);
            if (f.exists() && f.isDirectory()) {
                return userProfile;
            }
        }
        return null;
    }

    private String getKubeConfigFilename(String overrideFile) {
        // if an override file is given then, it becomes mandatory
        if (Objects.nonNull(overrideFile) && !overrideFile.isBlank()) {
            if (new File(overrideFile).isFile()) {
                return overrideFile;
            }
            throw new IllegalArgumentException(String.format("Override Kubernetes config file '%s' does not exist", overrideFile));
        }

        String fileName = System
            .getenv()
            .getOrDefault(KUBERNETES_KUBECONFIG_FILE, new File(getHomeDir(), ".kube" + File.separator + "config").toString());

        // if system property/env var contains multiple files take the first one based on the environment
        // we are running in (eg. : for Linux, ; for Windows)
        String[] fileNames = fileName.split(File.pathSeparator);

        if (fileNames.length > 1) {
            LOGGER.warn(
                "Found multiple Kubernetes config files [{}], using the first one: [{}]. If not desired file, please change it by doing `export KUBECONFIG=/path/to/kubeconfig` on Unix systems or `$Env:KUBECONFIG=/path/to/kubeconfig` on Windows.",
                fileNames,
                fileNames[0]
            );
            fileName = fileNames[0];
        }
        return fileName;
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    public boolean verifyHost() {
        return verifyHost;
    }

    public boolean useSSL() {
        return useSSL;
    }

    public String getAccessToken() {
        //reload only if at least 5 minutes have passed since the last reload
        if (isAccessTokenProjectionEnabled() && Instant.now().isAfter(this.accessTokenLastReload.plus(5, ChronoUnit.MINUTES))) {
            loadServiceAccountToken();
        }

        return accessToken;
    }

    /**
     * Returns the namespace under the following rules:
     * <ol>
     *     <li>If explicitly set via {@link #setCurrentNamespace(String)}. Either: /li>
     *     <ul>
     *         <li>When configured using using a config file, it will be the value of the namespace set for the current context or will be <code>"default"</code></li>
     *         <li>Externally set</li>
     *     </ul>
     *     <li>Loaded from the cluster information in which Gravitee is deployed</li>
     *     <li><code>null</code> if no context can be loaded from config file, or an error occurs reading cluster information</li>
     * </ol>
     *
     * @return the configured namespace
     */
    public String getCurrentNamespace() {
        if (currentNamespace == null) {
            loadCurrentNamespace();
        }
        return currentNamespace;
    }

    public void setMasterUrl(String masterUrl) {
        if (hasValue(masterUrl)) {
            this.masterUrl = masterUrl;
            this.setApiServerHost(masterUrl.substring(8, masterUrl.lastIndexOf(":"))); // skip initial "https://"
            this.setApiServerPort(Integer.parseInt(masterUrl.substring(masterUrl.lastIndexOf(':') + 1)));
        }
    }
}
