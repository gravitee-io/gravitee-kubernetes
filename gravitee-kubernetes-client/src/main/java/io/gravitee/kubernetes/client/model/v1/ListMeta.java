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
package io.gravitee.kubernetes.client.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListMeta {

    @JsonProperty("continue")
    private String continueValue;

    private Long remainingItemCount;
    private String resourceVersion;
    private String selfLink;

    /**
     * No args constructor for use in serialization
     *
     */
    public ListMeta() {}

    /**
     *
     * @param continueValue
     * @param remainingItemCount
     * @param resourceVersion
     * @param selfLink
     */
    public ListMeta(String continueValue, Long remainingItemCount, String resourceVersion, String selfLink) {
        super();
        this.continueValue = continueValue;
        this.remainingItemCount = remainingItemCount;
        this.resourceVersion = resourceVersion;
        this.selfLink = selfLink;
    }

    @JsonProperty("continue")
    public String getContinue() {
        return continueValue;
    }

    @JsonProperty("continue")
    public void setContinue(String continueValue) {
        this.continueValue = continueValue;
    }

    @JsonProperty("remainingItemCount")
    public Long getRemainingItemCount() {
        return remainingItemCount;
    }

    @JsonProperty("remainingItemCount")
    public void setRemainingItemCount(Long remainingItemCount) {
        this.remainingItemCount = remainingItemCount;
    }

    @JsonProperty("resourceVersion")
    public String getResourceVersion() {
        return resourceVersion;
    }

    @JsonProperty("resourceVersion")
    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    @JsonProperty("selfLink")
    public String getSelfLink() {
        return selfLink;
    }

    @JsonProperty("selfLink")
    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    @Override
    public String toString() {
        return (
            "ListMeta{" +
            "_continue='" +
            continueValue +
            '\'' +
            ", remainingItemCount=" +
            remainingItemCount +
            ", resourceVersion='" +
            resourceVersion +
            '\'' +
            ", selfLink='" +
            selfLink +
            '\'' +
            '}'
        );
    }
}
