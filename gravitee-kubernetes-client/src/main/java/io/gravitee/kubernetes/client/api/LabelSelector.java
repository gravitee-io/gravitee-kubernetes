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
public class LabelSelector {

    private final String name;
    private final String value;
    private final LabelSelector.Operator operator;

    private LabelSelector(String name, String value) {
        this(name, LabelSelector.Operator.EQUALS, value);
    }

    private LabelSelector(String name, LabelSelector.Operator operator, String value) {
        this.name = name;
        this.value = value;
        this.operator = operator;
    }

    public static LabelSelector equals(String name, String value) {
        return new LabelSelector(name, LabelSelector.Operator.EQUALS, value);
    }

    public static LabelSelector notEquals(String name, String value) {
        return new LabelSelector(name, LabelSelector.Operator.NOT_EQUALS, value);
    }

    @Override
    public String toString() {
        return URLEncoder.encode(String.format("%s%s%s", name, operator.getOperator(), value), StandardCharsets.UTF_8);
    }

    public String toQueryParam() {
        return "labelSelector=" + toString();
    }

    private enum Operator {
        EQUALS("="),
        NOT_EQUALS("!=");

        private final String operator;

        Operator(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }
    }
}
