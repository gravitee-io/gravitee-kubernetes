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
package io.gravitee.gateway.services.kube.crds.cache;

import io.gravitee.gateway.services.kube.crds.resources.PluginReference;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PluginRevision<T> {

    private final PluginReference pluginReference;
    private final long generation;
    private final T plugin;
    private final String hashCode;

    public PluginRevision(T plugin) {
        this(plugin, null, -1, null);
    }

    public PluginRevision(T plugin, PluginReference pluginReference, long generation, String hashCode) {
        this.pluginReference = pluginReference;
        this.generation = generation;
        this.plugin = plugin;
        this.hashCode = hashCode;
    }

    public PluginReference getPluginReference() {
        return pluginReference;
    }

    public long getGeneration() {
        return generation;
    }

    public T getPlugin() {
        return plugin;
    }

    public String getHashCode() {
        return hashCode;
    }

    public boolean isRef() {
        return this.pluginReference != null;
    }

    public boolean isValid() {
        return this.plugin != null;
    }
}
