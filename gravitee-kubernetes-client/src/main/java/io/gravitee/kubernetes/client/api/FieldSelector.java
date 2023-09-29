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
package io.gravitee.kubernetes.client.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FieldSelector {

    private final String name;
    private final String value;
    private final FieldSelector.Operator operator;

    private FieldSelector(String name, FieldSelector.Operator operator, String value) {
        this.name = name;
        this.value = value;
        this.operator = operator;
    }

    public static FieldSelector equals(String name, String value) {
        return new FieldSelector(name, FieldSelector.Operator.EQUALS, value);
    }

    public static FieldSelector notEquals(String name, String value) {
        return new FieldSelector(name, FieldSelector.Operator.NOT_EQUALS, value);
    }

    @Override
    public String toString() {
        return URLEncoder.encode(String.format("%s%s%s", name, operator.getValue(), value), StandardCharsets.UTF_8);
    }

    private enum Operator {
        EQUALS("="),
        NOT_EQUALS("!=");

        private final String value;

        Operator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
