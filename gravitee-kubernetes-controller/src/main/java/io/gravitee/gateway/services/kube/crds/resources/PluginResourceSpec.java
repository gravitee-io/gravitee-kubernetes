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
package io.gravitee.gateway.services.kube.crds.resources;

import io.gravitee.gateway.services.kube.crds.resources.plugin.Plugin;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PluginResourceSpec {

    private Map<String, Plugin> plugins = new LinkedHashMap<>();

    public PluginResourceSpec() {}

    public Optional<Plugin> getPlugin(String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public Map<String, Plugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, Plugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public String toString() {
        return "GraviteePluginSpec{" + "plugins=" + plugins + '}';
    }
}
