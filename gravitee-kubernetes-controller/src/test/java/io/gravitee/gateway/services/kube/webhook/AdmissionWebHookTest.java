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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.api.model.GroupVersionKindBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.gravitee.gateway.services.kube.crds.ResourceConstants;
import io.gravitee.gateway.services.kube.crds.resources.GatewayResource;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.crds.resources.ServicesResource;
import io.gravitee.gateway.services.kube.exceptions.ValidationException;
import io.gravitee.gateway.services.kube.services.GatewayResourceService;
import io.gravitee.gateway.services.kube.services.PluginsResourceService;
import io.gravitee.gateway.services.kube.services.ServicesResourceService;
import io.gravitee.gateway.services.kube.webhook.validator.Operation;
import io.gravitee.gateway.services.kube.webhook.validator.ResourceValidatorFactory;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { WebhookTestConfig.class })
public class AdmissionWebHookTest {

    @Autowired
    private AdmissionWebHook cut;

    @Autowired
    private PluginsResourceService pluginsResourceService;

    @Autowired
    private GatewayResourceService gatewayResourceService;

    @Autowired
    private ServicesResourceService servicesResourceService;

    @Autowired
    private ResourceValidatorFactory factory;

    @Before
    public void before() {
        reset(gatewayResourceService, pluginsResourceService, servicesResourceService, factory);
    }

    @Test
    public void shouldAcceptPlugin_CREATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CREATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyUpdated(any(), any());
        verify(pluginsResourceService, never()).maybeSafelyDeleted(any());
    }

    @Test
    public void shouldRejectPlugin_CREATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CREATE.name());

        doThrow(new ValidationException("shouldRejectPlugin_CREATE ERROR")).when(pluginsResourceService).maybeSafelyCreated(any());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should NOT be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyUpdated(any(), any());
        verify(pluginsResourceService, never()).maybeSafelyDeleted(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectPlugin_CREATE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptPlugin_UPDATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getOldObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.UPDATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService).maybeSafelyUpdated(any(), any());
        verify(pluginsResourceService, never()).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyDeleted(any());
    }

    @Test
    public void shouldRejectPlugin_UPDATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.UPDATE.name());

        doThrow(new ValidationException("shouldRejectPlugin_UPDATE ERROR")).when(pluginsResourceService).maybeSafelyUpdated(any(), any());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should NOT be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService).maybeSafelyUpdated(any(), any());
        verify(pluginsResourceService, never()).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyDeleted(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectPlugin_UPDATE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptPlugin_DELETE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getOldObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.DELETE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService).maybeSafelyDeleted(any());
        verify(pluginsResourceService, never()).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyUpdated(any(), any());
    }

    @Test
    public void shouldRejectPlugin_DELETE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.DELETE.name());

        doThrow(new ValidationException("shouldRejectPlugin_DELETE ERROR")).when(pluginsResourceService).maybeSafelyDeleted(any());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should NOT be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService).maybeSafelyDeleted(any());
        verify(pluginsResourceService, never()).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyUpdated(any(), any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectPlugin_DELETE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldRejectPlugin_CONNECT() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CONNECT.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should NOT be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(pluginsResourceService, never()).maybeSafelyCreated(any());
        verify(pluginsResourceService, never()).maybeSafelyUpdated(any(), any());
        verify(pluginsResourceService, never()).maybeSafelyDeleted(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "Operation 'CONNECT' not managed",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptGateway_CREATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CREATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService).maybeSafelyCreated(any());
        verify(gatewayResourceService, never()).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyDeleted(any());
    }

    @Test
    public void shouldRejectGateway_CREATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CREATE.name());

        doThrow(new ValidationException("shouldRejectGateway_CREATE ERROR")).when(gatewayResourceService).maybeSafelyCreated(any());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService).maybeSafelyCreated(any());
        verify(gatewayResourceService, never()).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyDeleted(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectGateway_CREATE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptGateway_UPDATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.UPDATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyCreated(any());
        verify(gatewayResourceService, never()).maybeSafelyDeleted(any());
    }

    @Test
    public void shouldRejectGateway_UPDATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.UPDATE.name());

        doThrow(new ValidationException("shouldRejectGateway_UPDATE ERROR")).when(gatewayResourceService).maybeSafelyUpdated(any());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyCreated(any());
        verify(gatewayResourceService, never()).maybeSafelyDeleted(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectGateway_UPDATE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptGateway_DELETE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.DELETE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService).maybeSafelyDeleted(any());
        verify(gatewayResourceService, never()).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyCreated(any());
    }

    @Test
    public void shouldRejectGateway_DELETE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.DELETE.name());

        doThrow(new ValidationException("shouldRejectGateway_DELETE ERROR")).when(gatewayResourceService).maybeSafelyDeleted(any());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService).maybeSafelyDeleted(any());
        verify(gatewayResourceService, never()).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyCreated(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectGateway_DELETE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldRejectGateway_CONNECT() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.GATEWAY_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new GatewayResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CONNECT.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(gatewayResourceService, never()).maybeSafelyCreated(any());
        verify(gatewayResourceService, never()).maybeSafelyUpdated(any());
        verify(gatewayResourceService, never()).maybeSafelyDeleted(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "Operation 'CONNECT' not managed",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptService_CREATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.SERVICES_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new ServicesResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CREATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(servicesResourceService).maybeSafelyUpdated(any());
    }

    @Test
    public void shouldRejectService_CREATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.SERVICES_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new ServicesResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.CREATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        doThrow(new ValidationException("shouldRejectService_CREATE ERROR")).when(servicesResourceService).maybeSafelyUpdated(any());

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(servicesResourceService).maybeSafelyUpdated(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectService_CREATE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAcceptService_UPDATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.SERVICES_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new ServicesResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.UPDATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(servicesResourceService).maybeSafelyUpdated(any());
    }

    @Test
    public void shouldRejectService_UPDATE() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.SERVICES_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new ServicesResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getOperation()).thenReturn(Operation.UPDATE.name());

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        doThrow(new ValidationException("shouldRejectService_UPDATE ERROR")).when(servicesResourceService).maybeSafelyUpdated(any());

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory).getValidator(any());
        verify(servicesResourceService).maybeSafelyUpdated(any());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());
        assertEquals(
            "Response should contain Status with message",
            "shouldRejectService_UPDATE ERROR",
            result.getResponse().getStatus().getMessage()
        );
    }

    @Test
    public void shouldAccept_WithoutValidation_ForStatusSubResource() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup(ResourceConstants.GROUP)
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);
        when(req.getSubResource()).thenReturn("Status");

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertTrue("Response UUID should be allowed", result.getResponse().getAllowed());

        verify(factory, never()).getValidator(any());
    }

    @Test
    public void shouldReject_InvalidResourceGroup() {
        final String uuid = UUID.randomUUID().toString();

        AdmissionRequest req = mock(AdmissionRequest.class);
        when(req.getRequestKind())
            .thenReturn(
                new GroupVersionKindBuilder()
                    .withGroup("UNKNOWN")
                    .withKind(ResourceConstants.PLUGINS_KIND)
                    .withVersion(ResourceConstants.DEFAULT_VERSION)
                    .build()
            );
        when(req.getObject()).thenReturn(new PluginResource());
        when(req.getUid()).thenReturn(uuid);

        AdmissionReview incomingReview = new AdmissionReview();
        incomingReview.setRequest(req);

        AdmissionReview result = cut.review(incomingReview);
        assertNotNull("Expected non null AdmissionReview in return", result);
        assertNotNull("Expected AdmissionReview with non null Response in return", result.getResponse());
        assertEquals("Response UUID should be the same as the request one", uuid, result.getResponse().getUid());
        assertFalse("Response UUID should NOT be allowed", result.getResponse().getAllowed());
        assertNotNull("Response should contain Status", result.getResponse().getStatus());
        assertEquals("Response should contain Status with code 400", Integer.valueOf(400), result.getResponse().getStatus().getCode());

        verify(factory, never()).getValidator(any());
    }
}
