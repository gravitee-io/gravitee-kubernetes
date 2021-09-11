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
package io.gravitee.kubernetes.client.model.config;

import com.fasterxml.jackson.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthInfo {

    @JsonProperty("as")
    private String as;

    @JsonProperty("as-groups")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> asGroups = new ArrayList<>();

    @JsonProperty("as-user-extra")
    private Map<String, ArrayList<String>> asUserExtra;

    @JsonProperty("auth-provider")
    private AuthProviderConfig authProvider;

    @JsonProperty("client-certificate")
    private String clientCertificate;

    @JsonProperty("client-certificate-data")
    private String clientCertificateData;

    @JsonProperty("client-key")
    private String clientKey;

    @JsonProperty("client-key-data")
    private String clientKeyData;

    @JsonProperty("extensions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Object> extensions = new ArrayList<>();

    @JsonProperty("password")
    private String password;

    @JsonProperty("token")
    private String token;

    @JsonProperty("tokenFile")
    private String tokenFile;

    @JsonProperty("username")
    private String username;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public AuthInfo() {}

    public AuthInfo(
        String as,
        List<String> asGroups,
        Map<String, ArrayList<String>> asUserExtra,
        AuthProviderConfig authProvider,
        String clientCertificate,
        String clientCertificateData,
        String clientKey,
        String clientKeyData,
        List<Object> extensions,
        String password,
        String token,
        String tokenFile,
        String username
    ) {
        super();
        this.as = as;
        this.asGroups = asGroups;
        this.asUserExtra = asUserExtra;
        this.authProvider = authProvider;
        this.clientCertificate = clientCertificate;
        this.clientCertificateData = clientCertificateData;
        this.clientKey = clientKey;
        this.clientKeyData = clientKeyData;
        this.extensions = extensions;
        this.password = password;
        this.token = token;
        this.tokenFile = tokenFile;
        this.username = username;
    }

    @JsonProperty("as")
    public String getAs() {
        return as;
    }

    @JsonProperty("as")
    public void setAs(String as) {
        this.as = as;
    }

    @JsonProperty("as-groups")
    public List<String> getAsGroups() {
        return asGroups;
    }

    @JsonProperty("as-groups")
    public void setAsGroups(List<String> asGroups) {
        this.asGroups = asGroups;
    }

    @JsonProperty("as-user-extra")
    public Map<String, ArrayList<String>> getAsUserExtra() {
        return asUserExtra;
    }

    @JsonProperty("as-user-extra")
    public void setAsUserExtra(Map<String, ArrayList<String>> asUserExtra) {
        this.asUserExtra = asUserExtra;
    }

    @JsonProperty("auth-provider")
    public AuthProviderConfig getAuthProvider() {
        return authProvider;
    }

    @JsonProperty("auth-provider")
    public void setAuthProvider(AuthProviderConfig authProvider) {
        this.authProvider = authProvider;
    }

    @JsonProperty("client-certificate")
    public String getClientCertificate() {
        return clientCertificate;
    }

    @JsonProperty("client-certificate")
    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    @JsonProperty("client-certificate-data")
    public String getClientCertificateData() {
        return clientCertificateData;
    }

    @JsonProperty("client-certificate-data")
    public void setClientCertificateData(String clientCertificateData) {
        this.clientCertificateData = clientCertificateData;
    }

    @JsonProperty("client-key")
    public String getClientKey() {
        return clientKey;
    }

    @JsonProperty("client-key")
    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    @JsonProperty("client-key-data")
    public String getClientKeyData() {
        return clientKeyData;
    }

    @JsonProperty("client-key-data")
    public void setClientKeyData(String clientKeyData) {
        this.clientKeyData = clientKeyData;
    }

    @JsonProperty("extensions")
    public List<Object> getExtensions() {
        return extensions;
    }

    @JsonProperty("extensions")
    public void setExtensions(List<Object> extensions) {
        this.extensions = extensions;
    }

    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("token")
    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("tokenFile")
    public String getTokenFile() {
        return tokenFile;
    }

    @JsonProperty("tokenFile")
    public void setTokenFile(String tokenFile) {
        this.tokenFile = tokenFile;
    }

    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    @JsonProperty("username")
    public void setUsername(String username) {
        this.username = username;
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
