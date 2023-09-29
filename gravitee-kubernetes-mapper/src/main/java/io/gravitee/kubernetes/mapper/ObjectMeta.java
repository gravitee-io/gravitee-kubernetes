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
package io.gravitee.kubernetes.mapper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class ObjectMeta {

    private final String name;
    private final Map<String, String> annotations = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();

    public ObjectMeta(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void addLabel(String label, String value) {
        labels.put(label, value);
    }

    public void addAnnotation(String annotation, String value) {
        annotations.put(annotation, value);
    }
}
