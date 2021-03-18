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
package io.gravitee.gateway.services.kube.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.BaseEncoding;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ControllerDigestHelper {

    public static String computePolicyHashCode(Policy policy) {
        String canonicalString = policy.getName() + ":" + policy.getConfiguration();
        return computeDigest(canonicalString);
    }

    public static String computeApiHashCode(Api api) {
        try {
            return computeDigest(Serialization.jsonMapper().writeValueAsString(api));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to process api definition", e);
        }
    }

    private static String computeDigest(String canonicalString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("sha-256");
            byte[] digestValue = digest.digest(canonicalString.getBytes());
            return BaseEncoding.base16().lowerCase().encode(digestValue, 0, 10);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algorithm 'sha-256' unsupported", e);
        }
    }

    public static String computeGenericConfigHashCode(HttpClientOptions client, Map<String, Object> ssl, Map<String, Object> proxy) {
        try {
            return computeDigest(Serialization.jsonMapper().writeValueAsString(Arrays.asList(client, ssl, proxy)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to process generic configuration map", e);
        }
    }

    public static String computeResourceHashCode(Resource resource) {
        String canonicalString = resource.getName() + ":" + resource.getType() + ":" + resource.getConfiguration();
        return computeDigest(canonicalString);
    }
}
