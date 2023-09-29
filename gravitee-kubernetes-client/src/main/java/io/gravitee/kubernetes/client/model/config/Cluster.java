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
package io.gravitee.kubernetes.client.model.config;

import com.fasterxml.jackson.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cluster {

    @JsonProperty("certificate-authority")
    private String certificateAuthority;

    @JsonProperty("certificate-authority-data")
    private String certificateAuthorityData;

    @JsonProperty("extensions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Object> extensions = new ArrayList<>();

    @JsonProperty("insecure-skip-tls-verify")
    private Boolean insecureSkipTlsVerify;

    @JsonProperty("proxy-url")
    private String proxyUrl;

    @JsonProperty("server")
    private String server;

    @JsonProperty("tls-server-name")
    private String tlsServerName;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public Cluster() {}

    public Cluster(
        String certificateAuthority,
        String certificateAuthorityData,
        List<Object> extensions,
        Boolean insecureSkipTlsVerify,
        String proxyUrl,
        String server,
        String tlsServerName
    ) {
        super();
        this.certificateAuthority = certificateAuthority;
        this.certificateAuthorityData = certificateAuthorityData;
        this.extensions = extensions;
        this.insecureSkipTlsVerify = insecureSkipTlsVerify;
        this.proxyUrl = proxyUrl;
        this.server = server;
        this.tlsServerName = tlsServerName;
    }

    @JsonProperty("certificate-authority")
    public String getCertificateAuthority() {
        return certificateAuthority;
    }

    @JsonProperty("certificate-authority")
    public void setCertificateAuthority(String certificateAuthority) {
        this.certificateAuthority = certificateAuthority;
    }

    @JsonProperty("certificate-authority-data")
    public String getCertificateAuthorityData() {
        return certificateAuthorityData;
    }

    @JsonProperty("certificate-authority-data")
    public void setCertificateAuthorityData(String certificateAuthorityData) {
        this.certificateAuthorityData = certificateAuthorityData;
    }

    @JsonProperty("extensions")
    public List<Object> getExtensions() {
        return extensions;
    }

    @JsonProperty("extensions")
    public void setExtensions(List<Object> extensions) {
        this.extensions = extensions;
    }

    @JsonProperty("insecure-skip-tls-verify")
    public Boolean getInsecureSkipTlsVerify() {
        return insecureSkipTlsVerify;
    }

    @JsonProperty("insecure-skip-tls-verify")
    public void setInsecureSkipTlsVerify(Boolean insecureSkipTlsVerify) {
        this.insecureSkipTlsVerify = insecureSkipTlsVerify;
    }

    @JsonProperty("proxy-url")
    public String getProxyUrl() {
        return proxyUrl;
    }

    @JsonProperty("proxy-url")
    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    @JsonProperty("server")
    public String getServer() {
        return server;
    }

    @JsonProperty("server")
    public void setServer(String server) {
        this.server = server;
    }

    @JsonProperty("tls-server-name")
    public String getTlsServerName() {
        return tlsServerName;
    }

    @JsonProperty("tls-server-name")
    public void setTlsServerName(String tlsServerName) {
        this.tlsServerName = tlsServerName;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
