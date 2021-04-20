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

package com.googlesource.gerrit.plugins.pubsub.local;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.inject.Inject;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.ConsumerExecutor;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;
import com.googlesource.gerrit.plugins.pubsub.SubscriberProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public class LocalSubscriberProvider extends SubscriberProvider {
  private EnvironmentChecker environmentChecker;

  @Inject
  public LocalSubscriberProvider(
      PubSubConfiguration pubSubProperties,
      CredentialsProvider credentials,
      EnvironmentChecker environmentChecker,
      @ConsumerExecutor ScheduledExecutorService executor) {
    super(credentials, pubSubProperties, executor);
    this.environmentChecker = environmentChecker;
  }

  @Override
  public Subscriber get(String topic, MessageReceiver receiver) throws IOException {
    TransportChannelProvider channelProvider = createChannelProvider();
    createTopic(channelProvider, pubSubProperties.getGCloudProject(), topic);
    return Subscriber.newBuilder(getOrCreateSubscription(topic).getName(), receiver)
        .setChannelProvider(channelProvider)
        .setExecutorProvider(FixedExecutorProvider.create(executor))
        .setCredentialsProvider(credentials)
        .build();
  }

  @Override
  protected SubscriptionAdminSettings createSubscriptionAdminSettings() throws IOException {
    TransportChannelProvider channelProvider = createChannelProvider();
    return SubscriptionAdminSettings.newBuilder()
        .setTransportChannelProvider(channelProvider)
        .setCredentialsProvider(credentials)
        .build();
  }

  private TransportChannelProvider createChannelProvider() {
    ManagedChannel channel =
        ManagedChannelBuilder.forTarget(environmentChecker.getLocalHostAndPort().get())
            .usePlaintext()
            .build();
    TransportChannelProvider channelProvider =
        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    return channelProvider;
  }

  private static void createTopic(
      TransportChannelProvider channelProvider, String project, String topicId) throws IOException {

    TopicAdminSettings topicAdminSettings =
        TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build();
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
      TopicName topicName = TopicName.of(project, topicId);
      topicAdminClient.createTopic(topicName);
    } catch (AlreadyExistsException e) {
      // topic already exists do nothing
    }
  }
}
