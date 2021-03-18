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

import static io.gravitee.gateway.services.kube.utils.K8SResourceUtils.getFullName;

import io.fabric8.kubernetes.client.CustomResource;
import io.gravitee.gateway.services.kube.crds.cache.PluginRevision;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WatchActionContext<T extends CustomResource> {

    private boolean reloading = false;

    private T resource;
    private final Event event;

    private final List<PluginRevision<?>> pluginRevisions = new ArrayList<>();
    private String httpConfigHashCode = "";

    public WatchActionContext(T resource, Event event) {
        this.resource = resource;
        this.event = event;
    }

    // package visibility to allow refresh only into services impl
    final WatchActionContext<T> refreshResource(T updatedResource) {
        this.resource = updatedResource;
        return this;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public T getResource() {
        return resource;
    }

    public Event getEvent() {
        return event;
    }

    public String getNamespace() {
        return this.resource.getMetadata().getNamespace();
    }

    public long getGeneration() {
        if (this.resource.getMetadata() != null || this.resource.getMetadata().getGeneration() != null) {
            return this.resource.getMetadata().getGeneration();
        }
        return -1;
    }

    public String getResourceName() {
        return this.resource.getMetadata().getName();
    }

    public String getResourceFullName() {
        return getFullName(this.resource.getMetadata());
    }

    public void addAllRevisions(List<PluginRevision<?>> pluginRevisions) {
        this.pluginRevisions.addAll(pluginRevisions);
    }

    public List<PluginRevision<?>> getPluginRevisions() {
        return pluginRevisions;
    }

    public String getHttpConfigHashCode() {
        return httpConfigHashCode;
    }

    public void setHttpConfigHashCode(String httpConfigHashCode) {
        this.httpConfigHashCode = httpConfigHashCode;
    }

    public enum Event {
        ADDED,
        MODIFIED,
        DELETED,
        REFERENCE_UPDATED,
        NONE,
    }
}
