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
package io.gravitee.gateway.services.kube.services;

import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Policy;
import io.gravitee.gateway.services.kube.KubeSyncTestConfig;
import io.gravitee.gateway.services.kube.crds.cache.PluginCacheManager;
import io.gravitee.gateway.services.kube.crds.cache.PluginRevision;
import io.gravitee.gateway.services.kube.crds.resources.PluginReference;
import io.gravitee.gateway.services.kube.crds.resources.PluginResource;
import io.gravitee.gateway.services.kube.crds.status.IntegrationState;
import io.gravitee.gateway.services.kube.crds.status.PluginResourceStatus;
import io.gravitee.gateway.services.kube.exceptions.PipelineException;
import io.gravitee.gateway.services.kube.exceptions.ValidationException;
import io.gravitee.gateway.services.kube.services.impl.WatchActionContext;
import io.gravitee.gateway.services.kube.services.listeners.PluginsResourceListener;
import io.gravitee.gateway.services.kube.utils.ObjectMapperHelper;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
@ContextConfiguration(classes = KubeSyncTestConfig.class)
public class PluginsServiceTestResource extends AbstractServiceTest {

    @Autowired
    protected PluginsResourceService cut;

    @Autowired
    protected PluginCacheManager pluginCacheManager;

    protected PluginsResourceListener listener = mock(PluginsResourceListener.class);

    @Before
    public void prepareTest() {
        reset(listener);
        cut.registerListener(listener);
        pluginCacheManager.clearCache();
    }

    @Test
    public void processAction_Should_ReplaceSecretInPlugin() {
        populateSecret("default", "myapp", "/kubernetes/test-secret-opaque.yml");
        populatePluginResource("default", "myapp-plugins", "/kubernetes/plugins/test-gravitee-plugin-success.yml", true);

        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        TestSubscriber<WatchActionContext<PluginResource>> observable = cut
            .processAction(new WatchActionContext<>(plugins, WatchActionContext.Event.ADDED))
            .test();
        observable.awaitTerminalEvent();
        observable.assertNoErrors();
        observable.assertValue(
            ctx -> {
                boolean valid = ctx.getEvent() == WatchActionContext.Event.ADDED;
                valid = valid && (ctx.getPluginRevisions().size() == 6);
                valid =
                    valid &&
                    ctx
                        .getPluginRevisions()
                        .stream()
                        .filter(p -> p.getPlugin() instanceof Policy)
                        .filter(p -> ((Policy) p.getPlugin()).getName().equals("jwt"))
                        // DA7OLkdACP is the decoded value of the secret
                        .findFirst()
                        .map(p -> ((Policy) p.getPlugin()).getConfiguration().contains("DA7OLkdACP"))
                        .orElseGet(() -> false);
                return valid;
            }
        );

        verify(listener).onPluginsUpdate(any());
    }

    @Test
    public void processAction_ShouldNot_NotifyListenerTwice() {
        populateSecret("default", "myapp", "/kubernetes/test-secret-opaque.yml", 2);
        populatePluginResource("default", "myapp-plugins", "/kubernetes/plugins/test-gravitee-plugin-success.yml", true, 2);

        final IntegrationState integration = new IntegrationState();
        integration.setState(IntegrationState.State.SUCCESS);

        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        PluginResourceStatus status = new PluginResourceStatus();
        status.setIntegration(integration);
        status.setHashCodes(new HashMap<>());
        plugins.setStatus(status);

        PluginResource pluginsSecond = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        PluginResourceStatus statusSecond = new PluginResourceStatus();
        statusSecond.setIntegration(integration);
        Map<String, String> hashCodes = new HashMap<>();
        hashCodes.put("jwt-poc", "f4624fabb81070eea9ca");
        hashCodes.put("oauth2-resource", "25e3a96dea07b801f9a5");
        hashCodes.put("rate-limit", "08693024974124779d1a");
        hashCodes.put("quota-policy", "9006c8a7761ad4c46d61");
        hashCodes.put("key-less-poc", "0d13ed4fc2b0a5aeaa8d");
        hashCodes.put("oauth2", "6a4601ff81416188b6dc");
        statusSecond.setHashCodes(hashCodes);
        pluginsSecond.setStatus(statusSecond);

        TestSubscriber<WatchActionContext<PluginResource>> observable = cut
            .processAction(new WatchActionContext<>(plugins, WatchActionContext.Event.MODIFIED))
            .test();
        observable.awaitTerminalEvent();
        observable.assertNoErrors();
        observable.assertValue(
            ctx -> {
                boolean valid = ctx.getEvent() == WatchActionContext.Event.MODIFIED;
                valid = valid && (ctx.getPluginRevisions().size() == 6);
                valid =
                    valid &&
                    ctx
                        .getPluginRevisions()
                        .stream()
                        .filter(p -> p.getPlugin() instanceof Policy)
                        .filter(p -> ((Policy) p.getPlugin()).getName().equals("jwt"))
                        // DA7OLkdACP is the decoded value of the secret
                        .findFirst()
                        .map(p -> ((Policy) p.getPlugin()).getConfiguration().contains("DA7OLkdACP"))
                        .orElseGet(() -> false);
                return valid;
            }
        );

        observable = cut.processAction(new WatchActionContext<>(pluginsSecond, WatchActionContext.Event.MODIFIED)).test();
        observable.awaitTerminalEvent();
        observable.assertNoErrors();
        observable.assertValue(
            ctx -> {
                boolean valid = ctx.getEvent() == WatchActionContext.Event.MODIFIED;
                valid = valid && (ctx.getPluginRevisions().size() == 6);
                valid =
                    valid &&
                    ctx
                        .getPluginRevisions()
                        .stream()
                        .filter(p -> p.getPlugin() instanceof Policy)
                        .filter(p -> ((Policy) p.getPlugin()).getName().equals("jwt"))
                        // DA7OLkdACP is the decoded value of the secret
                        .findFirst()
                        .map(p -> ((Policy) p.getPlugin()).getConfiguration().contains("DA7OLkdACP"))
                        .orElseGet(() -> false);
                return valid;
            }
        );

        // same resource GraviteePlugin is processed twice, listener called only once
        verify(listener, times(1)).onPluginsUpdate(any());
    }

    @Test
    public void processAction_ShouldFail_UnknownSecret() {
        populatePluginResource("default", "myapp-plugins", "/kubernetes/plugins/test-gravitee-plugin-unknown-secret.yml", true);

        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-unknown-secret.yml",
            PluginResource.class
        );
        TestSubscriber<WatchActionContext<PluginResource>> observable = cut
            .processAction(new WatchActionContext<>(plugins, WatchActionContext.Event.ADDED))
            .test();
        observable.awaitTerminalEvent();
        observable.assertError(
            error -> error instanceof PipelineException && error.getMessage().startsWith("Unable to read key 'myapp-unknown-password'")
        );

        verify(listener, never()).onPluginsUpdate(any());
    }

    @Test
    public void maybeSafelyCreated_ShouldValidate() {
        populateSecret("default", "myapp", "/kubernetes/test-secret-opaque.yml");
        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );

        cut.maybeSafelyCreated(plugins);
    }

    @Test(expected = ValidationException.class)
    public void maybeSafelyCreated_ShouldNot_ValidateCreate_MissingSecret() {
        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-unknown-secret.yml",
            PluginResource.class
        );
        cut.maybeSafelyCreated(plugins);
    }

    @Test
    public void maybeSafelyUpdate_ShouldValidate_cacheEmpty() {
        populateSecret("default", "myapp", "/kubernetes/test-secret-opaque.yml");
        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        // load same resource but remove the 'rate-limit' plugin
        PluginResource oldPlugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        oldPlugins.getSpec().getPlugins().remove("rate-limit");

        // validation pass, removed plugin isn't used (plugin cache is empty
        cut.maybeSafelyUpdated(plugins, oldPlugins);
    }

    @Test
    public void maybeSafelyUpdate_ShouldValidate_RemovedPluginNotUsed() {
        populateSecret("default", "myapp", "/kubernetes/test-secret-opaque.yml");
        PluginResource oldPlugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        // load same resource but remove the 'rate-limit' plugin in the most recent GraviteePlugin
        plugins.getSpec().getPlugins().remove("rate-limit");

        PluginReference ref1 = createPluginReference("jwt-poc", "myapp-plugins", "default");
        PluginReference ref2 = createPluginReference("oauth2", "myapp-plugins", "default");
        pluginCacheManager.registerPluginsFor("some-id", Arrays.asList(new PluginRevision<Policy>(null, ref1, 1, "")));
        pluginCacheManager.registerPluginsFor("some-other-id", Arrays.asList(new PluginRevision<Policy>(null, ref2, 1, "")));

        // validation pass, removed plugin isn't used (plugin cache is empty
        cut.maybeSafelyUpdated(plugins, oldPlugins);
    }

    @Test(expected = ValidationException.class)
    public void maybeSafelyUpdate_ShouldNot_Validate_RemovedPluginIsUsed() {
        populateSecret("default", "myapp", "/kubernetes/test-secret-opaque.yml");
        PluginResource oldPlugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        // load same resource but remove the 'rate-limit' plugin in the most recent GraviteePlugin
        plugins.getSpec().getPlugins().remove("rate-limit");

        PluginReference ref1 = createPluginReference("jwt-poc", "myapp-plugins", "default");
        PluginReference ref2 = createPluginReference("rate-limit", "myapp-plugins", "default");
        pluginCacheManager.registerPluginsFor("some-id", Arrays.asList(new PluginRevision<Policy>(null, ref1, 1, "")));
        pluginCacheManager.registerPluginsFor("some-other-id", Arrays.asList(new PluginRevision<Policy>(null, ref2, 1, "")));

        // validation pass, removed plugin isn't used (plugin cache is empty
        cut.maybeSafelyUpdated(plugins, oldPlugins);
    }

    @Test
    public void maybeSafelyDelete_ShouldValidate_RemovedPluginNotUsed() {
        PluginResource oldPlugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        PluginReference ref1 = createPluginReference("plugin-from-other-resource", "myapp-plugins", "default");
        pluginCacheManager.registerPluginsFor("some-id", Arrays.asList(new PluginRevision<Policy>(null, ref1, 1, "")));

        cut.maybeSafelyDeleted(oldPlugins);
    }

    @Test(expected = ValidationException.class)
    public void maybeSafelyDelete_ShouldNot_Validate_RemovedPluginIsUsed() {
        PluginResource plugins = ObjectMapperHelper.readYamlAs(
            "/kubernetes/plugins/test-gravitee-plugin-success.yml",
            PluginResource.class
        );
        PluginReference ref2 = createPluginReference("rate-limit", "myapp-plugins", "default");
        pluginCacheManager.registerPluginsFor("some-other-id", Arrays.asList(new PluginRevision<Policy>(null, ref2, 1, "")));

        cut.maybeSafelyDeleted(plugins);
    }

    private PluginReference createPluginReference(String name, String res, String ns) {
        PluginReference pluginRef = new PluginReference();
        pluginRef.setName(name);
        pluginRef.setNamespace(ns);
        pluginRef.setResource(res);
        return pluginRef;
    }
}
