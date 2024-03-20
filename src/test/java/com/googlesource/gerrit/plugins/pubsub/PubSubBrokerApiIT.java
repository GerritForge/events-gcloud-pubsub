// Copyright (C) 2021 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.pubsub;

import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.WaitUtil;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGson;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.local.EnvironmentChecker;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Test;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

@NoHttpd
@TestPlugin(
    name = "events-gcloud-pubsub",
    sysModule = "com.googlesource.gerrit.plugins.pubsub.Module")
public class PubSubBrokerApiIT extends LightweightPluginDaemonTest {
  private static final String PROJECT_ID = "test_project";
  private static final String TOPIC_ID = "test_topic";
  private static final String GROUP_ID = "test_group";
  private static final String SUBSCRIPTION_ID = "test_subscription_id";

  private static final Duration TEST_TIMEOUT = Duration.ofSeconds(5);
  private static final String PRIVATE_KEY_LOCATION = "not used in test";
  private static final String DEFAULT_INSTANCE_ID = "instance-id";

  @Inject @EventGson private Gson gson;

  private TransportChannelProvider channelProvider;
  private NoCredentialsProvider credentialsProvider;
  private ManagedChannel channel;

  private BrokerApi objectUnderTest;

  public PubSubEmulatorContainer emulator =
      new PubSubEmulatorContainer(
          DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators"));

  @Override
  public void setUpTestPlugin() throws Exception {
    emulator.start();
    String hostport = emulator.getEmulatorEndpoint();
    System.setProperty(EnvironmentChecker.PUBSUB_EMULATOR_HOST, hostport);
    channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build();
    channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    credentialsProvider = NoCredentialsProvider.create();

    createTopic(TOPIC_ID, channelProvider, credentialsProvider);

    super.setUpTestPlugin();

    objectUnderTest = plugin.getSysInjector().getInstance(BrokerApi.class);
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
  public void shouldSendEvent() throws IOException {
    createSubscription(SUBSCRIPTION_ID, TOPIC_ID, channelProvider, credentialsProvider);
    Event event = new ProjectCreatedEvent();
    event.instanceId = DEFAULT_INSTANCE_ID;
    String expectedMessageJson = gson.toJson(event);

    objectUnderTest.send(TOPIC_ID, event);

    readMessageAndValidate(
        (pullResponse) -> {
          assertThat(pullResponse.getReceivedMessagesList()).hasSize(1);
          assertThat(pullResponse.getReceivedMessages(0).getMessage().getData().toStringUtf8())
              .isEqualTo(expectedMessageJson);
        });
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.sendStreamEvents", value = "true")
  public void shouldProduceStreamEvents() throws Exception {
    String subscriptionId = "gerrit-subscription-id";
    String topicId = "gerrit";
    createSubscription(subscriptionId, topicId, channelProvider, credentialsProvider);

    createChange();

    readMessageAndValidate(
        (pullResponse) -> {
          List<ReceivedMessage> messages = pullResponse.getReceivedMessagesList();
          assertThat(messages).hasSize(4);
          Map<String, Long> messageTypeCount =
              messages.stream()
                  .map(m -> gson.fromJson(m.getMessage().getData().toStringUtf8(), Map.class))
                  .map(m -> m.get("type").toString())
                  .collect(groupingBy(t -> t, counting()));

          assertThat(messageTypeCount.get(RefUpdatedEvent.TYPE)).isEqualTo(3);
          assertThat(messageTypeCount.get("patchset-created")).isEqualTo(1);
        },
        PROJECT_ID,
        subscriptionId);
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.sendStreamEvents", value = "false")
  public void shouldNotProduceStreamEventsWhenDisabled() throws Exception {
    String subscriptionId = "gerrit-subscription-id";
    String topicId = "gerrit";
    createSubscription(subscriptionId, topicId, channelProvider, credentialsProvider);

    createChange();

    readMessageAndValidate(
        (pullResponse) -> assertThat(pullResponse.getReceivedMessagesList()).isEmpty(),
        PROJECT_ID,
        subscriptionId);
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  public void shouldConsumeEvent() throws InterruptedException {
    Event event = new ProjectCreatedEvent();
    event.instanceId = DEFAULT_INSTANCE_ID;
    String expectedMessageJson = gson.toJson(event);
    TestConsumer consumer = new TestConsumer();

    objectUnderTest.receiveAsync(TOPIC_ID, consumer);

    objectUnderTest.send(TOPIC_ID, event);

    WaitUtil.waitUntil(
        () ->
            consumer.getMessage() != null
                && expectedMessageJson.equals(gson.toJson(consumer.getMessage())),
        TEST_TIMEOUT);
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  public void shouldConsumeEventWithGroupId() throws InterruptedException {
    Event event = new ProjectCreatedEvent();
    event.instanceId = DEFAULT_INSTANCE_ID;
    String expectedMessageJson = gson.toJson(event);
    TestConsumer consumer = new TestConsumer();

    objectUnderTest.receiveAsync(TOPIC_ID, GROUP_ID, consumer);

    objectUnderTest.send(TOPIC_ID, event);

    assertThat(countSubscribers()).isEqualTo(1);
    WaitUtil.waitUntil(
        () ->
            consumer.getMessage() != null
                && expectedMessageJson.equals(gson.toJson(consumer.getMessage())),
        TEST_TIMEOUT);
  }

  @Test
  @GerritConfig(name = "plugin.events-gcloud-pubsub.gcloudProject", value = PROJECT_ID)
  @GerritConfig(name = "plugin.events-gcloud-pubsub.subscriptionId", value = SUBSCRIPTION_ID)
  @GerritConfig(
      name = "plugin.events-gcloud-pubsub.privateKeyLocation",
      value = PRIVATE_KEY_LOCATION)
  public void shouldDisconnectOnlySubscriberForSpecificGroupId() throws Exception {
    String streamName = "stream_name";
    String groupId1 = "group1";
    String groupId2 = "group2";
    TestConsumer consumer = new TestConsumer();

    objectUnderTest.receiveAsync(streamName, groupId1, consumer);
    objectUnderTest.receiveAsync(streamName, groupId2, consumer);

    objectUnderTest.disconnect(streamName, groupId2);

    assertThat(countSubscribers(streamName, groupId2)).isEqualTo(0L);
    assertThat(countSubscribers(streamName, groupId1)).isEqualTo(1L);
  }

  private void readMessageAndValidate(Consumer<PullResponse> validate) throws IOException {
    readMessageAndValidate(validate, PROJECT_ID, SUBSCRIPTION_ID);
  }

  private void readMessageAndValidate(
      Consumer<PullResponse> validate, String projectId, String subscriptionId) throws IOException {
    SubscriberStubSettings subscriberStubSettings =
        SubscriberStubSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
      PullRequest pullRequest =
          PullRequest.newBuilder()
              .setMaxMessages(Integer.MAX_VALUE) // make sure that we read all messages
              .setSubscription(ProjectSubscriptionName.format(projectId, subscriptionId))
              .build();
      PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

      validate.accept(pullResponse);
    }
  }

  private void createTopic(
      String topicId,
      TransportChannelProvider channelProvider,
      NoCredentialsProvider credentialsProvider)
      throws IOException {
    TopicAdminSettings topicAdminSettings =
        TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
      TopicName topicName = TopicName.of(PROJECT_ID, topicId);
      topicAdminClient.createTopic(topicName);
    }
  }

  private void createSubscription(
      String subscriptionId,
      String topicId,
      TransportChannelProvider channelProvider,
      NoCredentialsProvider credentialsProvider)
      throws IOException {
    SubscriptionAdminSettings subscriptionAdminSettings =
        SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();
    SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(subscriptionAdminSettings);
    ProjectSubscriptionName subscriptionName =
        ProjectSubscriptionName.of(PROJECT_ID, subscriptionId);
    subscriptionAdminClient
        .createSubscription(
            subscriptionName,
            TopicName.of(PROJECT_ID, topicId),
            PushConfig.getDefaultInstance(),
            10)
        .getName();
  }

  private long countSubscribers() {
    return countSubscribers(TOPIC_ID, GROUP_ID);
  }

  private long countSubscribers(String topicId, String groupId) {
    return objectUnderTest.topicSubscribersWithGroupId().stream()
        .filter(t -> groupId.equals(t.groupId()) && topicId.equals(t.topicSubscriber().topic()))
        .count();
  }

  private class TestConsumer implements Consumer<Event> {
    private Event msg;

    @Override
    public void accept(Event msg) {
      this.msg = msg;
    }

    public Event getMessage() {
      return msg;
    }
  }
}
