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
package io.gravitee.kubernetes.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.gravitee.kubernetes.client.model.config.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public class KubernetesConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesConfig.class);

    public static final String KUBERNETES_SERVICE_HOST_PROPERTY = "KUBERNETES_SERVICE_HOST";
    public static final String KUBERNETES_SERVICE_PORT_HTTPS_PROPERTY = "KUBERNETES_SERVICE_PORT_HTTPS";
    public static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH = "KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH";
    public static final String KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH = "KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH";
    public static final String KUBERNETES_CURRENT_NAMESPACE_PATH = "KUBERNETES_CURRENT_NAMESPACE";
    public static final String KUBERNETES_KUBECONFIG_FILE = "kubeconfig";
    public static final Long DEFAULT_WEBSOCKET_TIMEOUT = 5 * 60 * 1000L;

    private String apiServerHost;
    private int apiServerPort;
    private String caCertData;
    private boolean verifyHost = true;
    private boolean useSSL = true;
    private String accessToken;
    private String currentNamespace;
    private long websocketTimeout = DEFAULT_WEBSOCKET_TIMEOUT;

    private String masterUrl;
    private String apiVersion = "v1";
    private String clientCertData;
    private String clientKeyData;
    private File file;
    private String username;
    private String password;

    private static KubernetesConfig instance;

    private KubernetesConfig() {
        if (!tryKubeConfig() && !tryServiceAccount()) {
            LOGGER.error("Unable to configure Kubernetes Config. No KubeConfig or Service account is found");
        }
    }

    public static synchronized KubernetesConfig getInstance() {
        if (instance == null) {
            instance = new KubernetesConfig();
        }
        return instance;
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
    private boolean loadServiceAccountToken() {
        String tokenFilePath = getSystemPropertyOrEnvVar(
            KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH,
            "/var/run/secrets/kubernetes.io/serviceaccount/token"
        );
        try {
            boolean serviceAccountAccessTokenExists = Files.isRegularFile(new File(tokenFilePath).toPath());
            if (serviceAccountAccessTokenExists) {
                LOGGER.debug("Found service account token at: [{}].", tokenFilePath);
                this.setAccessToken(new String(Files.readAllBytes(new File(tokenFilePath).toPath())));
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
        LOGGER.debug("Trying to configure client from Kubernetes config...");
        File kubeConfigFile = new File(getKubeConfigFilename());
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
            Cluster currentCluster = getCluster(config, currentContext);
            if (currentCluster != null) {
                this.setMasterUrl(currentCluster.getServer());
                this.setCaCertData(new String(Base64.getDecoder().decode(currentCluster.getCertificateAuthorityData())));
                AuthInfo currentAuthInfo = getUserAuthInfo(config, currentContext);
                if (currentAuthInfo != null) {
                    this.setClientCertData(currentAuthInfo.getClientCertificateData());
                    this.setClientKeyData(currentAuthInfo.getClientKeyData());
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

    private <T> String getSystemPropertyOrEnvVar(String propertyName, T defaultValue) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isEmpty()) {
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
            if (userProfile != null && !userProfile.isEmpty()) {
                File f = new File(userProfile);
                if (f.exists() && f.isDirectory()) {
                    return userProfile;
                }
            }
        }
        String home = System.getenv("HOME");
        if (home != null && !home.isEmpty()) {
            File f = new File(home);
            if (f.exists() && f.isDirectory()) {
                return home;
            }
        }

        //Fall back to user.home should never really get here
        return System.getProperty("user.home", ".");
    }

    private String getKubeConfigFilename() {
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

    public boolean verifyHost() {
        return verifyHost;
    }

    public void setVerifyHost(boolean verifyHost) {
        this.verifyHost = verifyHost;
    }

    public boolean useSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getCurrentNamespace() {
        if (currentNamespace == null) {
            loadCurrentNamespace();
        }
        return currentNamespace;
    }

    public void setCurrentNamespace(String currentNamespace) {
        this.currentNamespace = currentNamespace;
    }

    public long getWebsocketTimeout() {
        return websocketTimeout;
    }

    public void setWebsocketTimeout(long websocketTimeout) {
        this.websocketTimeout = websocketTimeout;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public void setMasterUrl(String masterUrl) {
        if (masterUrl != null && !masterUrl.isEmpty()) {
            this.masterUrl = masterUrl;
            this.setApiServerHost(masterUrl.substring(8, masterUrl.lastIndexOf(":"))); // skip initial "https://"
            this.setApiServerPort(Integer.parseInt(masterUrl.substring(masterUrl.lastIndexOf(':') + 1)));
        }
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getClientCertData() {
        return clientCertData;
    }

    public void setClientCertData(String clientCertData) {
        this.clientCertData = clientCertData;
    }

    public String getClientKeyData() {
        return clientKeyData;
    }

    public void setClientKeyData(String clientKeyData) {
        this.clientKeyData = clientKeyData;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
