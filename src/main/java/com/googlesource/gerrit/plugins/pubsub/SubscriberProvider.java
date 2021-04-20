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
  protected ScheduledExecutorService executor;

  @Inject
  public SubscriberProvider(
      CredentialsProvider credentials,
      PubSubConfiguration pubSubProperties,
      @ConsumerExecutor ScheduledExecutorService executor) {
    this.credentials = credentials;
    this.pubSubProperties = pubSubProperties;
    this.executor = executor;
  }

  public Subscriber get(String topic, MessageReceiver receiver) throws IOException {
    return Subscriber.newBuilder(getOrCreateSubscription(topic).getName(), receiver)
        .setExecutorProvider(FixedExecutorProvider.create(executor))
        .setCredentialsProvider(credentials)
        .build();
  }

  protected SubscriptionAdminSettings createSubscriptionAdminSettings() throws IOException {
    return SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
  }

  protected Subscription getOrCreateSubscription(String topicId) throws IOException {
    try (SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(createSubscriptionAdminSettings())) {
      String subscriptionName =
          String.format("%s-%s", pubSubProperties.getSubscriptionId(), topicId);
      ProjectSubscriptionName projectSubscriptionName =
          ProjectSubscriptionName.of(pubSubProperties.getGCloudProject(), subscriptionName);

      return getSubscription(subscriptionAdminClient, projectSubscriptionName)
          .orElseGet(
              () ->
                  subscriptionAdminClient.createSubscription(
                      createSubscriptionRequest(projectSubscriptionName, topicId)));
    }
  }

  protected Subscription createSubscriptionRequest(
      ProjectSubscriptionName projectSubscriptionName, String topicId) {
    return Subscription.newBuilder()
        .setName(projectSubscriptionName.toString())
        .setTopic(TopicName.of(pubSubProperties.getGCloudProject(), topicId).toString())
        .setAckDeadlineSeconds(pubSubProperties.getAckDeadlineSeconds())
        .setRetainAckedMessages(true)
        .build();
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
}
