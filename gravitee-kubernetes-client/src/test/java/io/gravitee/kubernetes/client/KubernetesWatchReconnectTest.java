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
package io.gravitee.kubernetes.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.impl.KubernetesClientV1Impl;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.UpgradeRejectedException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The watch must survive a 401 on reconnect (expired service account token).
 */
public class KubernetesWatchReconnectTest {

    private static final String WATCH_PATH = "/api/v1/namespaces/test/secrets?fieldSelector=metadata.name%3Dsecret1&watch=true";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private KubernetesMockServer server;
    private KubernetesClient kubernetesClient;
    private Path tokenFile;

    @Before
    public void before() throws IOException {
        server = new KubernetesMockServer(true);
        server.init();

        tokenFile = folder.newFile("token").toPath();
        Files.writeString(tokenFile, "expired-token");

        KubernetesConfig config = KubernetesConfig.newInstance();
        config.setApiServerHost(server.getHostName());
        config.setApiServerPort(server.getPort());
        config.setVerifyHost(false);
        config.loadServiceAccountToken(tokenFile.toString());
        kubernetesClient = new KubernetesClientV1Impl(config);
    }

    @After
    public void after() {
        server.destroy();
    }

    @Test
    public void should_keep_watching_with_rotated_token_after_401() throws Exception {
        // The kubelet has rotated the token on disk, but the client still holds the expired one.
        Files.writeString(tokenFile, "rotated-token");
        server.expect().get().withPath(WATCH_PATH).andReturn(401, "Unauthorized").once();
        server
            .expect()
            .get()
            .withPath(WATCH_PATH)
            .andUpgradeToWebSocket()
            .open()
            .waitFor(10L)
            .andEmit(new WatchEvent(secret("1"), "MODIFIED"))
            .done()
            .once();

        TestSubscriber<Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .test();

        // awaitCount gives up after 5 seconds
        assertThat(obs.awaitCount(1).values()).as("event received after the 401 has been retried").hasSize(1);
        obs.cancel();

        RecordedRequest rejected = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest accepted = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(rejected).isNotNull();
        assertThat(accepted).isNotNull();
        assertThat(rejected.getHeaders().values("Authorization")).containsExactly("Bearer expired-token");
        assertThat(accepted.getHeaders().values("Authorization")).containsExactly("Bearer rotated-token");
    }

    @Test
    public void should_fail_watch_when_token_is_rejected_and_not_rotated() {
        server.expect().get().withPath(WATCH_PATH).andReturn(401, "Unauthorized").always();

        TestSubscriber<Event<io.gravitee.kubernetes.client.model.v1.Secret>> obs = kubernetesClient
            .watch(WatchQuery.<io.gravitee.kubernetes.client.model.v1.Secret>from("/test/secrets/secret1").build())
            .test();

        // The watch fails instead of hanging silently, so that callers can retry
        obs.awaitDone(5, TimeUnit.SECONDS).assertError(UpgradeRejectedException.class);
    }

    private static Secret secret(String resourceVersion) {
        return new SecretBuilder()
            .withNewMetadata()
            .withNamespace("test")
            .withName("secret1")
            .withResourceVersion(resourceVersion)
            .endMetadata()
            .build();
    }
}
