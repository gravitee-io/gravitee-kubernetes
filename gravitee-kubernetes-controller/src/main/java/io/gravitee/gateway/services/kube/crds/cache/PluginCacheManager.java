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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import io.gravitee.gateway.services.kube.crds.resources.PluginReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PluginCacheManager {

    private final Map<String, List<PluginRevision<?>>> pluginsByResource = new ConcurrentHashMap<>();

    public List<PluginRevision<?>> getPluginsUsedBy(String ref) {
        return ofNullable(pluginsByResource.get(ref)).orElse(emptyList());
    }

    public void removePluginsUsedBy(String ref) {
        pluginsByResource.remove(ref);
    }

    public void registerPluginsFor(String ref, List<PluginRevision<?>> plugins) {
        pluginsByResource.put(ref, plugins);
    }

    public void registerPluginsFor(Map<String, List<PluginRevision<?>>> plugins) {
        pluginsByResource.putAll(plugins);
    }

    public List<String> resourcesUsingPlugin(PluginReference ref) {
        return pluginsByResource
            .entrySet()
            .stream()
            .filter(
                entry ->
                    entry.getValue().stream().map(PluginRevision::getPluginReference).filter(r -> r.equals(ref)).findFirst().isPresent()
            )
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public void clearCache() {
        this.pluginsByResource.clear();
    }
}
