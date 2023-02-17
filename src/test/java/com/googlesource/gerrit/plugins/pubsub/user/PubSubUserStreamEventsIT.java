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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.acceptance.testsuite.project.TestProjectUpdate.allowCapability;
import static com.google.gerrit.server.group.SystemGroupBackend.REGISTERED_USERS;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import com.google.api.gax.rpc.NotFoundException;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.acceptance.testsuite.project.ProjectOperations;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.pubsub.TopicProvider;
import com.googlesource.gerrit.plugins.pubsub.local.EnvironmentChecker;
import com.googlesource.gerrit.plugins.pubsub.local.LocalTopicProvider;
import com.googlesource.gerrit.plugins.pubsub.rest.SubscribePubSubStreamEventsCapability;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

@TestPlugin(
    name = "events-gcloud-pubsub",
    sysModule = "com.googlesource.gerrit.plugins.pubsub.Module")
public class PubSubUserStreamEventsIT extends LightweightPluginDaemonTest {
  private static final String PROJECT_ID = "test_project";
  private static final String SUBSCRIPTION_ID = "test_subscription_id";

  private static final String PRIVATE_KEY_LOCATION = "not used in test";

  @Inject @EventGson private Gson gson;
  @Inject private ProjectOperations projectOperations;

  private ManagedChannel channel;
  private TopicProvider topicProvider;

  public PubSubEmulatorContainer emulator =
      new PubSubEmulatorContainer(
          DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators"));

  @Override
  public void setUpTestPlugin() throws Exception {
    emulator.start();
    String hostport = emulator.getEmulatorEndpoint();
    System.setProperty(EnvironmentChecker.PUBSUB_EMULATOR_HOST, hostport);
    channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();

    super.setUpTestPlugin();
    topicProvider = plugin.getSysInjector().getInstance(LocalTopicProvider.class);
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
}
