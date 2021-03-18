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
package io.gravitee.gateway.services.kube.webhook;

import static io.gravitee.gateway.services.kube.crds.ResourceConstants.*;
import static java.lang.String.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.*;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.services.kube.webhook.validator.ResourceValidatorFactory;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AdmissionWebHook implements ManagementEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdmissionWebHook.class);
    private static final ObjectMapper mapper = Serialization.jsonMapper();

    private static final String STATUS_SUB_RESOURCE = "Status";

    @Autowired
    private ResourceValidatorFactory factory;

    @Override
    public boolean isWebhook() {
        return true;
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    @Override
    public String path() {
        return "/kube/admission";
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext
            .request()
            .bodyHandler(
                handler -> {
                    AdmissionReview reviewResponse = processRequest(handler);
                    sendResponse(routingContext, reviewResponse);
                }
            );
    }

    protected final AdmissionReview processRequest(Buffer body) {
        AdmissionReview reviewResponse;
        try {
            reviewResponse = review(mapper.readValue(body.getBytes(), AdmissionReview.class));
        } catch (IOException e) {
            LOGGER.error("AdmissionReview object can't be read", e);
            Status status = new Status();
            status.setCode(500);
            status.setMessage("AdmissionReview object can't be read");
            reviewResponse =
                new AdmissionReviewBuilder()
                    .withApiVersion("admission.k8s.io/v1")
                    .withNewResponse()
                    .withAllowed(false)
                    .withStatus(status)
                    .endResponse()
                    .build();
        }
        return reviewResponse;
    }

    private void sendResponse(RoutingContext routingContext, AdmissionReview reviewResponse) {
        try {
            routingContext.response().setStatusCode(200);
            routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            routingContext.response().end(mapper.writeValueAsString(reviewResponse));
        } catch (JsonProcessingException e) {
            routingContext.response().setStatusCode(500);
            routingContext.response().end("Error while processing review response");
        }
    }

    protected final AdmissionReview review(AdmissionReview incomingReview) {
        AdmissionRequest request = incomingReview.getRequest();

        AdmissionResponse response = new AdmissionResponseBuilder().withUid(request.getUid()).withAllowed(true).build();

        // check that resource belongs to Gravitee.io Group
        if (!GROUP.equals(request.getRequestKind().getGroup())) {
            // this shouldn't be possible because Group is used as key to register bean deserializer
            response =
                badRequestResponse(
                    request.getUid(),
                    format("Group '%s' unsupported, expected '%s'", request.getRequestKind().getGroup(), GROUP)
                );
        } else if (!STATUS_SUB_RESOURCE.equalsIgnoreCase(request.getSubResource())) {
            // Bypass validation if demand is related to the Status part of the resource.
            // Status update is triggered by the controller, resource definition shouldn't have changed

            // NOTE: maybe used as event in audit log.
            LOGGER.debug(
                "Operation '{}' requested by '{}' on Resource '{}' with name '{}' (Namespace: {})",
                request.getOperation(),
                request.getUserInfo(),
                request.getResource(),
                request.getName(),
                request.getNamespace()
            );

            final Status status = factory.getValidator(request).validate(request.getOperation());

            if (status.getCode() > 200) {
                response = invalidRequestResponse(request.getUid(), status);
            }
        }

        return new AdmissionReviewBuilder().withApiVersion(incomingReview.getApiVersion()).withResponse(response).build();
    }

    private AdmissionResponse badRequestResponse(String reqUuid, String message) {
        AdmissionResponse response;
        Status status = new Status();
        status.setCode(400);
        status.setMessage(message);
        response = new AdmissionResponseBuilder().withUid(reqUuid).withAllowed(false).withStatus(status).build();
        return response;
    }

    private AdmissionResponse invalidRequestResponse(String reqUuid, Status status) {
        AdmissionResponse response;
        response = new AdmissionResponseBuilder().withUid(reqUuid).withAllowed(false).withStatus(status).build();
        return response;
    }
}
