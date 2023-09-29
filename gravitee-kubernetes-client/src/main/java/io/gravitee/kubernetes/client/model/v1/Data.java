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

import java.util.Base64;
import java.util.Map;

public class Data {

    private final Map<String, String> data;

    public Data(Map<String, String> data) {
        this.data = data;
    }
    /*
    public String getBase64(String key) {
        String value = data.get(key);
        return (value == null) ? value : new String(Base64.getDecoder().decode(value));
    }

    public byte[] getBinary(String key) {
        String value = data.get(key);
        return (value == null) ? value : new String(Base64.getDecoder().decode(value));
    }

    public String getString(String key) {
        String value = data.get(key);
        return (value == null) ? value : new String(Base64.getDecoder().decode(value));
    }
     */
}
