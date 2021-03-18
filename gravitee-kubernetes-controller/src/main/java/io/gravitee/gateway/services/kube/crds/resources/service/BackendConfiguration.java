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
package io.gravitee.gateway.services.kube.crds.resources.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.HttpProxyType;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import java.util.Map;
import java.util.Optional;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BackendConfiguration {

    @JsonProperty("httpProxy")
    private Map<String, Object> httpProxy;

    @JsonProperty("httpClient")
    private HttpClientOptions httpClientOptions;

    @JsonProperty("httpSslOptions")
    private Map<String, Object> httpClientSslOptions;

    public BackendConfiguration() {}

    public Map<String, Object> getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(Map<String, Object> httpProxy) {
        this.httpProxy = httpProxy;
    }

    public HttpClientOptions getHttpClientOptions() {
        return httpClientOptions;
    }

    public void setHttpClientOptions(HttpClientOptions httpClientOptions) {
        this.httpClientOptions = httpClientOptions;
    }

    public Map<String, Object> getHttpClientSslOptions() {
        return this.httpClientSslOptions;
    }

    public void setHttpClientSslOptions(Map<String, Object> httpClientSslOptions) {
        this.httpClientSslOptions = httpClientSslOptions;
    }

    public static Optional<HttpClientSslOptions> buildHttpClientSslOptions(Map<String, Object> options) {
        Optional<HttpClientSslOptions> result = Optional.empty();
        if (options != null && !options.isEmpty()) {
            HttpClientSslOptions sslConf = new HttpClientSslOptions();
            if (options.containsKey("trustAll")) {
                sslConf.setTrustAll((boolean) options.get("trustAll"));
            }
            if (options.containsKey("hostnameVerifier")) {
                sslConf.setHostnameVerifier((boolean) options.get("hostnameVerifier"));
            }
            if (options.containsKey("trustStore")) {
                Map<String, Object> trustStore = (Map<String, Object>) options.get("trustStore");
                String type = (String) trustStore.get("type");
                if ("PEM".equalsIgnoreCase(type)) {
                    PEMTrustStore pemTrustStore = new PEMTrustStore();
                    pemTrustStore.setContent((String) trustStore.get("content"));
                    pemTrustStore.setPath((String) trustStore.get("path"));
                    sslConf.setTrustStore(pemTrustStore);
                }
                if ("PKCS12".equalsIgnoreCase(type)) {
                    PKCS12TrustStore pkcs12TrustStore = new PKCS12TrustStore();
                    pkcs12TrustStore.setContent((String) trustStore.get("content"));
                    pkcs12TrustStore.setPath((String) trustStore.get("path"));
                    pkcs12TrustStore.setPassword((String) trustStore.get("password"));
                    sslConf.setTrustStore(pkcs12TrustStore);
                }
                if ("JKS".equalsIgnoreCase(type)) {
                    JKSTrustStore jksTrustStore = new JKSTrustStore();
                    jksTrustStore.setContent((String) trustStore.get("content"));
                    jksTrustStore.setPath((String) trustStore.get("path"));
                    jksTrustStore.setPassword((String) trustStore.get("password"));
                    sslConf.setTrustStore(jksTrustStore);
                }
            }
            if (options.containsKey("keyStore")) {
                Map<String, Object> keyStore = (Map<String, Object>) options.get("keyStore");
                String type = (String) keyStore.get("type");
                if ("PEM".equalsIgnoreCase(type)) {
                    PEMKeyStore pemKeyStore = new PEMKeyStore();
                    pemKeyStore.setKeyPath((String) keyStore.get("keyPath"));
                    pemKeyStore.setKeyContent((String) keyStore.get("keyContent"));
                    pemKeyStore.setCertPath((String) keyStore.get("certPath"));
                    pemKeyStore.setCertContent((String) keyStore.get("certContent"));
                    sslConf.setKeyStore(pemKeyStore);
                }
                if ("PKCS12".equalsIgnoreCase(type)) {
                    PKCS12KeyStore pkcs12KeyStore = new PKCS12KeyStore();
                    pkcs12KeyStore.setContent((String) keyStore.get("content"));
                    pkcs12KeyStore.setPath((String) keyStore.get("path"));
                    pkcs12KeyStore.setPassword((String) keyStore.get("password"));
                    sslConf.setKeyStore(pkcs12KeyStore);
                }
                if ("JKS".equalsIgnoreCase(type)) {
                    JKSKeyStore jksKeyStore = new JKSKeyStore();
                    jksKeyStore.setContent((String) keyStore.get("content"));
                    jksKeyStore.setPath((String) keyStore.get("path"));
                    jksKeyStore.setPassword((String) keyStore.get("password"));
                    sslConf.setKeyStore(jksKeyStore);
                }
            }
            result = Optional.of(sslConf);
        }
        return result;
    }

    public static Optional<HttpProxy> buildHttpProxy(Map<String, Object> options) {
        Optional<HttpProxy> result = Optional.empty();
        if (options != null && !options.isEmpty()) {
            HttpProxy proxyConf = new HttpProxy();
            if (options.containsKey("enabled")) {
                proxyConf.setEnabled((boolean) options.get("enabled"));
            }
            if (options.containsKey("useSystemProxy")) {
                proxyConf.setUseSystemProxy((boolean) options.get("useSystemProxy"));
            }
            if (options.containsKey("host")) {
                proxyConf.setHost((String) options.get("host"));
            }
            if (options.containsKey("port")) {
                proxyConf.setPort((int) options.get("port"));
            }
            if (options.containsKey("username")) {
                proxyConf.setUsername((String) options.get("username"));
            }
            if (options.containsKey("password")) {
                proxyConf.setPassword((String) options.get("password"));
            }
            if (options.containsKey("type")) {
                proxyConf.setType(HttpProxyType.valueOf((String) options.get("type")));
            }
            result = Optional.of(proxyConf);
        }
        return result;
    }
}
