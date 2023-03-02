// Copyright (C) 2023 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.pubsub.user;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allowCapability;
import static com.google.gerrit.server.group.SystemGroupBackend.REGISTERED_USERS;
import static org.junit.Assert.assertThrows;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.api.gax.rpc.NotFoundException;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestAccount;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.pubsub.v1.Subscription;
import com.googlesource.gerrit.plugins.pubsub.TopicProvider;
import com.googlesource.gerrit.plugins.pubsub.local.EnvironmentChecker;
import com.googlesource.gerrit.plugins.pubsub.local.LocalTopicProvider;
import com.googlesource.gerrit.plugins.pubsub.rest.SubscribePubSubStreamEventsCapability;
import com.googlesource.gerrit.plugins.pubsub.rest.SubscriptionInput;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

@TestPlugin(
    name = "events-gcloud-pubsub",
    sysModule = "com.googlesource.gerrit.plugins.pubsub.Module")
public class PubSubUserStreamEventsIT extends LightweightPluginDaemonTest {
  private static final String PROJECT_ID = "test_project";
  private static final String SUBSCRIPTION_ID = "test_subscription_id";
  private static final String PRIVATE_KEY_LOCATION = "not used in test";
  private static final Pattern SUBSCRIPTION_PATTERN =
      Pattern.compile(".*(projects/.*/subscriptions/stream-events[0-9A-Za-z-]+).*");
  private static final int PORT = 18888;

  private final CountDownLatch expectedRequestLatch = new CountDownLatch(1);

  @Inject @EventGson private Gson gson;
  @Inject private ProjectOperations projectOperations;

  private ManagedChannel channel;
  private TopicProvider topicProvider;
  private PubSubUserSubscriptionProvider subscriptionProvider;

  public PubSubEmulatorContainer emulator =
      new PubSubEmulatorContainer(
          DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators"));
  @Rule public WireMockRule wireMockRule = new WireMockRule(options().port(PORT));

  @Override
  public void setUpTestPlugin() throws Exception {
    Testcontainers.exposeHostPorts(PORT);
    emulator.start();
    String hostport = emulator.getEmulatorEndpoint();
    System.setProperty(EnvironmentChecker.PUBSUB_EMULATOR_HOST, hostport);
    channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();

    givenThat(any(anyUrl()).willReturn(aResponse().withStatus(200)));

    super.setUpTestPlugin();
    topicProvider = plugin.getSysInjector().getInstance(LocalTopicProvider.class);
    subscriptionProvider =
        plugin.getSysInjector().getInstance(PubSubUserSubscriptionProvider.class);
  }

  @Override
  public void tearDownTestPlugin() {
    channel.shutdown();
    emulator.close();
    super.tearDownTestPlugin();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "false")
  public void shouldNotExposeRestApiForUserIfUserStreamEventsAreDisabled() throws Exception {
    adminRestSession.put("/accounts/self/pubsub.topic").assertNotFound();
    adminRestSession.delete("/accounts/self/pubsub.topic").assertNotFound();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldCreateTopicForAdminUser() throws Exception {
    adminRestSession.put("/accounts/self/pubsub.topic").assertCreated();
    assertThat(topicProvider.getForAccount(admin.id())).isNotNull();
    adminRestSession.put("/accounts/self/pubsub.topic").assertNoContent();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldCreateTopicForUserOnlyWithPermission() throws Exception {
    userRestSession.put("/accounts/self/pubsub.topic").assertForbidden();
    projectOperations
        .allProjectsForUpdate()
        .add(
            allowCapability("events-gcloud-pubsub-" + SubscribePubSubStreamEventsCapability.ID)
                .group(REGISTERED_USERS))
        .update();
    userRestSession.put("/accounts/self/pubsub.topic").assertCreated();
    assertThat(topicProvider.getForAccount(user.id())).isNotNull();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldPermitCreateTopicForOtherUserOnlyForServerMaintainers() throws Exception {
    TestAccount user2 = accountCreator.user2();
    projectOperations
        .allProjectsForUpdate()
        .add(
            allowCapability("events-gcloud-pubsub-" + SubscribePubSubStreamEventsCapability.ID)
                .group(REGISTERED_USERS))
        .update();
    userRestSession
        .put(String.format("/accounts/%d/pubsub.topic", user2.id().get()))
        .assertForbidden();
    projectOperations
        .allProjectsForUpdate()
        .add(allowCapability(GlobalCapability.MAINTAIN_SERVER).group(REGISTERED_USERS))
        .update();
    userRestSession
        .put(String.format("/accounts/%d/pubsub.topic", user2.id().get()))
        .assertCreated();
    assertThat(topicProvider.getForAccount(user2.id())).isNotNull();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldDeleteTopicForUser() throws Exception {
    adminRestSession.put("/accounts/self/pubsub.topic").assertCreated();
    assertThat(topicProvider.getForAccount(admin.id())).isNotNull();
    adminRestSession.delete("/accounts/self/pubsub.topic").assertNoContent();
    assertThrows(NotFoundException.class, () -> topicProvider.getForAccount(admin.id()));
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldDeleteTopicForUserOnlyWithPermission() throws Exception {
    adminRestSession
        .put(String.format("/accounts/%d/pubsub.topic", user.id().get()))
        .assertCreated();
    userRestSession.delete("/accounts/self/pubsub.topic").assertForbidden();
    projectOperations
        .allProjectsForUpdate()
        .add(
            allowCapability("events-gcloud-pubsub-" + SubscribePubSubStreamEventsCapability.ID)
                .group(REGISTERED_USERS))
        .update();
    userRestSession.delete("/accounts/self/pubsub.topic").assertNoContent();
    assertThrows(NotFoundException.class, () -> topicProvider.getForAccount(user.id()));
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldPermitDeleteTopicForOtherUserOnlyForServerMaintainers() throws Exception {
    TestAccount user2 = accountCreator.user2();
    adminRestSession
        .put(String.format("/accounts/%d/pubsub.topic", user2.id().get()))
        .assertCreated();
    projectOperations
        .allProjectsForUpdate()
        .add(
            allowCapability("events-gcloud-pubsub-" + SubscribePubSubStreamEventsCapability.ID)
                .group(REGISTERED_USERS))
        .update();
    userRestSession
        .delete(String.format("/accounts/%d/pubsub.topic", user2.id().get()))
        .assertForbidden();
    projectOperations
        .allProjectsForUpdate()
        .add(allowCapability(GlobalCapability.MAINTAIN_SERVER).group(REGISTERED_USERS))
        .update();
    userRestSession
        .delete(String.format("/accounts/%d/pubsub.topic", user2.id().get()))
        .assertNoContent();
    assertThrows(NotFoundException.class, () -> topicProvider.getForAccount(user.id()));
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldCreateSubscriptionForAdminUser() throws Exception {
    SubscriptionInput input = new SubscriptionInput();
    input.pushEndpoint = String.format("host.testcontainers.internal:%s/events", PORT);
    input.verificationToken = "test1234";
    adminRestSession.put("/accounts/self/pubsub.topic").assertCreated();
    RestResponse resp = adminRestSession.put("/accounts/self/pubsub.sub", input);
    resp.assertCreated();
    Matcher matcher = SUBSCRIPTION_PATTERN.matcher(resp.getEntityContent());
    matcher.find();
    Optional<Subscription> sub = subscriptionProvider.getSubscription(matcher.group(1));
    assertThat(sub.isPresent()).isTrue();
    assertThat(sub.get().getPushConfig().getPushEndpoint())
        .isEqualTo(String.format("http://%s?token=test1234", input.pushEndpoint));
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldCreateSubscriptionForUserOnlyWithPermission() throws Exception {
    SubscriptionInput input = new SubscriptionInput();
    input.pushEndpoint = "localhost:8081/events";
    input.verificationToken = "test1234";
    adminRestSession.put(String.format("/accounts/%d/pubsub.topic", user.id().get()));
    userRestSession.put("/accounts/self/pubsub.sub", input).assertForbidden();
    projectOperations
        .allProjectsForUpdate()
        .add(
            allowCapability("events-gcloud-pubsub-" + SubscribePubSubStreamEventsCapability.ID)
                .group(REGISTERED_USERS))
        .update();
    RestResponse resp = userRestSession.put("/accounts/self/pubsub.sub", input);
    resp.assertCreated();
    Matcher matcher = SUBSCRIPTION_PATTERN.matcher(resp.getEntityContent());
    matcher.find();
    assertThat(subscriptionProvider.getSubscription(matcher.group(1)).isPresent()).isTrue();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldPermitCreateSubscriptionForOtherUserOnlyForServerMaintainers()
      throws Exception {
    TestAccount user2 = accountCreator.user2();
    SubscriptionInput input = new SubscriptionInput();
    input.pushEndpoint = "localhost:8081/events";
    input.verificationToken = "test1234";
    adminRestSession.put(String.format("/accounts/%d/pubsub.topic", user2.id().get()));
    projectOperations
        .allProjectsForUpdate()
        .add(
            allowCapability("events-gcloud-pubsub-" + SubscribePubSubStreamEventsCapability.ID)
                .group(REGISTERED_USERS))
        .update();
    userRestSession
        .put(String.format("/accounts/%d/pubsub.sub", user2.id().get()), input)
        .assertForbidden();
    projectOperations
        .allProjectsForUpdate()
        .add(allowCapability(GlobalCapability.MAINTAIN_SERVER).group(REGISTERED_USERS))
        .update();
    RestResponse resp =
        userRestSession.put(String.format("/accounts/%d/pubsub.sub", user2.id().get()), input);
    resp.assertCreated();
    Matcher matcher = SUBSCRIPTION_PATTERN.matcher(resp.getEntityContent());
    matcher.find();
    assertThat(subscriptionProvider.getSubscription(matcher.group(1)).isPresent()).isTrue();
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.enableUserStreamEvents", value = "true")
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.serviceAccountForUserSubs",
      value = "service@account.example.com")
  public void shouldReceiveEventsInClient() throws Exception {
    SubscriptionInput input = new SubscriptionInput();
    input.pushEndpoint = String.format("host.testcontainers.internal:%s/events", PORT);
    input.verificationToken = "test1234";
    adminRestSession.put("/accounts/self/pubsub.topic").assertCreated();
    adminRestSession.put("/accounts/self/pubsub.sub", input).assertCreated();

    wireMockRule.addMockServiceRequestListener(
        (request, response) -> {
          if (request
              .getAbsoluteUrl()
              .contains(String.format("%s?token=test1234", input.pushEndpoint))) {
            expectedRequestLatch.countDown();
          }
        });
    createChange();
    assertThat(expectedRequestLatch.await(5, TimeUnit.SECONDS)).isTrue();
  }
}
