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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectMeta {

    private Map<String, String> annotations;
    private String clusterName;
    private String creationTimestamp;
    private Long deletionGracePeriodSeconds;
    private String deletionTimestamp;
    private List<String> finalizers = new ArrayList<String>();
    private String generateName;
    private Long generation;
    private Map<String, String> labels;
    private String name;
    private String namespace;
    private String resourceVersion;
    private String selfLink;
    private String uid;

    /**
     * No args constructor for use in serialization
     *
     */
    public ObjectMeta() {}

    /**
     *
     * @param generation
     * @param finalizers
     * @param resourceVersion
     * @param annotations
     * @param generateName
     * @param deletionTimestamp
     * @param labels
     * @param selfLink
     * @param deletionGracePeriodSeconds
     * @param uid
     * @param clusterName
     * @param creationTimestamp
     * @param name
     * @param namespace
     */
    public ObjectMeta(
        Map<String, String> annotations,
        String clusterName,
        String creationTimestamp,
        Long deletionGracePeriodSeconds,
        String deletionTimestamp,
        List<String> finalizers,
        String generateName,
        Long generation,
        Map<String, String> labels,
        String name,
        String namespace,
        String resourceVersion,
        String selfLink,
        String uid
    ) {
        super();
        this.annotations = annotations;
        this.clusterName = clusterName;
        this.creationTimestamp = creationTimestamp;
        this.deletionGracePeriodSeconds = deletionGracePeriodSeconds;
        this.deletionTimestamp = deletionTimestamp;
        this.finalizers = finalizers;
        this.generateName = generateName;
        this.generation = generation;
        this.labels = labels;
        this.name = name;
        this.namespace = namespace;
        this.resourceVersion = resourceVersion;
        this.selfLink = selfLink;
        this.uid = uid;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Long getDeletionGracePeriodSeconds() {
        return deletionGracePeriodSeconds;
    }

    public void setDeletionGracePeriodSeconds(Long deletionGracePeriodSeconds) {
        this.deletionGracePeriodSeconds = deletionGracePeriodSeconds;
    }

    public String getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(String deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    public List<String> getFinalizers() {
        return finalizers;
    }

    public void setFinalizers(List<String> finalizers) {
        this.finalizers = finalizers;
    }

    public String getGenerateName() {
        return generateName;
    }

    public void setGenerateName(String generateName) {
        this.generateName = generateName;
    }

    public Long getGeneration() {
        return generation;
    }

    public void setGeneration(Long generation) {
        this.generation = generation;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public String toString() {
        return (
            "ObjectMeta{" +
            "annotations=" +
            annotations +
            ", clusterName='" +
            clusterName +
            '\'' +
            ", creationTimestamp='" +
            creationTimestamp +
            '\'' +
            ", deletionGracePeriodSeconds=" +
            deletionGracePeriodSeconds +
            ", deletionTimestamp='" +
            deletionTimestamp +
            '\'' +
            ", finalizers=" +
            finalizers +
            ", generateName='" +
            generateName +
            '\'' +
            ", generation=" +
            generation +
            ", labels=" +
            labels +
            ", name='" +
            name +
            '\'' +
            ", namespace='" +
            namespace +
            '\'' +
            ", resourceVersion='" +
            resourceVersion +
            '\'' +
            ", selfLink='" +
            selfLink +
            '\'' +
            ", uid='" +
            uid +
            '\'' +
            '}'
        );
    }
}
