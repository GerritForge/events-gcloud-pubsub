// Copyright (C) 2025 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.pubsub;

import com.gerritforge.gerrit.eventbroker.EventsBrokerConfiguration;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.GetSubscriptionRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.SeekRequest;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class SubscriberProvider {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected CredentialsProvider credentials;
  protected PubSubConfiguration pubSubProperties;
  protected EventsBrokerConfiguration eventsBrokerConfiguration;
  protected ScheduledExecutorService executor;

  @Inject
  public SubscriberProvider(
      CredentialsProvider credentials,
      PubSubConfiguration pubSubProperties,
      EventsBrokerConfiguration eventsBrokerConfiguration,
      @ConsumerExecutor ScheduledExecutorService executor) {
    this.credentials = credentials;
    this.pubSubProperties = pubSubProperties;
    this.eventsBrokerConfiguration = eventsBrokerConfiguration;
    this.executor = executor;
  }

  public Subscriber get(String topic, String groupId, MessageReceiver receiver) throws IOException {
    return get(topic, groupId, Optional.empty(), receiver);
  }

  public Subscriber get(
      String topic, String groupId, Optional<String> partition, MessageReceiver receiver)
      throws IOException {
    return Subscriber.newBuilder(
            getOrCreateSubscription(topic, groupId, partition).getName(), receiver)
        .setExecutorProvider(FixedExecutorProvider.create(executor))
        .setCredentialsProvider(credentials)
        .build();
  }

  protected SubscriptionAdminSettings createSubscriptionAdminSettings() throws IOException {
    return SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
  }

  protected Subscription getOrCreateSubscription(String topicId) throws IOException {
    return getOrCreateSubscription(topicId, pubSubProperties.getSubscriptionId());
  }

  protected Subscription getOrCreateSubscription(String topicId, String groupId)
      throws IOException {
    return getOrCreateSubscription(topicId, groupId, Optional.empty());
  }

  protected Subscription getOrCreateSubscription(
      String topicId, String groupId, Optional<String> partition) throws IOException {
    try (SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(createSubscriptionAdminSettings())) {
      String subscriptionName = String.format("%s-%s", groupId, topicId);
      ProjectSubscriptionName projectSubscriptionName =
          ProjectSubscriptionName.of(pubSubProperties.getGCloudProject(), subscriptionName);

      Optional<String> filter = partition.map(value -> subscriptionFilter(topicId, value));
      Optional<Subscription> subscription =
          getSubscription(subscriptionAdminClient, projectSubscriptionName);
      if (subscription.isPresent()) {
        if (filter.isPresent() && !filter.get().equals(subscription.get().getFilter())) {
          throw new IllegalStateException(
              String.format(
                  "Subscription %s has filter '%s', expected '%s'",
                  projectSubscriptionName, subscription.get().getFilter(), filter.get()));
        }
        return subscription.get();
      }
      return subscriptionAdminClient.createSubscription(
          createSubscriptionRequest(projectSubscriptionName, topicId, filter));
    }
  }

  protected Subscription createSubscriptionRequest(
      ProjectSubscriptionName projectSubscriptionName, String topicId) {
    return createSubscriptionRequest(projectSubscriptionName, topicId, Optional.empty());
  }

  protected Subscription createSubscriptionRequest(
      ProjectSubscriptionName projectSubscriptionName, String topicId, Optional<String> filter) {
    Subscription.Builder subscription =
        Subscription.newBuilder()
            .setName(projectSubscriptionName.toString())
            .setTopic(TopicName.of(pubSubProperties.getGCloudProject(), topicId).toString())
            .setAckDeadlineSeconds(pubSubProperties.getAckDeadlineSeconds())
            .setRetainAckedMessages(true);
    filter.ifPresent(subscription::setFilter);
    return subscription.build();
  }

  protected Optional<Subscription> getSubscription(
      SubscriptionAdminClient subscriptionAdminClient,
      ProjectSubscriptionName projectSubscriptionName) {
    try {
      // we should use subscriptionAdminClient.listSubscriptions but for local setup this method
      // throws UNKNOWN_EXCEPTION
      return Optional.of(subscriptionAdminClient.getSubscription(projectSubscriptionName));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  public void replayMessages(String subscriptionName) {
    try (SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(createSubscriptionAdminSettings())) {
      Duration messageRetentionDuration =
          subscriptionAdminClient
              .getSubscription(
                  GetSubscriptionRequest.newBuilder().setSubscription(subscriptionName).build())
              .getMessageRetentionDuration();
      LocalDateTime retentionTime =
          LocalDateTime.now().minusSeconds(messageRetentionDuration.getSeconds());
      Timestamp retentionTimeEpoch =
          Timestamp.newBuilder()
              .setSeconds(retentionTime.atZone(ZoneOffset.UTC).toEpochSecond())
              .build();

      SeekRequest request =
          SeekRequest.newBuilder()
              .setSubscription(subscriptionName)
              .setTime(retentionTimeEpoch)
              .build();
      subscriptionAdminClient.seek(request);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Cannot replay messages");
    }
  }

  private String subscriptionFilter(String topic, String partition) {
    String partitionProperty =
        eventsBrokerConfiguration
            .getEventPropertyForTopic(topic)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format("No partition property configured for topic %s", topic)));
    return String.format("attributes.%s = \"%s\"", partitionProperty, partition);
  }
}
