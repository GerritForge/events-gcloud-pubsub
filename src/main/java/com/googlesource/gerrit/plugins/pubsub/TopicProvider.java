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

package com.googlesource.gerrit.plugins.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.entities.Account;
import com.google.inject.Inject;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.rest.PubSubUserTopicNameFactory;
import java.io.IOException;

public class TopicProvider {
  protected final TopicAdminSettings topicAdminSettings;
  protected final PubSubUserTopicNameFactory topicNameFactory;

  @Inject
  public TopicProvider(CredentialsProvider credentials, PubSubUserTopicNameFactory topicNameFactory)
      throws IOException {
    this.topicAdminSettings =
        TopicAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
    this.topicNameFactory = topicNameFactory;
  }

  public TopicProvider(
      TransportChannelProvider channelProvider,
      CredentialsProvider credentials,
      PubSubUserTopicNameFactory topicNameFactory)
      throws IOException {
    this.topicAdminSettings =
        TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentials)
            .build();
    this.topicNameFactory = topicNameFactory;
  }

  public Topic createForAccount(Account.Id accountId) throws IOException {
    return create(topicNameFactory.createForAccount(accountId));
  }

  public Topic create(TopicName name) throws IOException {
    try (TopicAdminClient topicAdminClient = getTopicAdminClient()) {
      return topicAdminClient.createTopic(name);
    }
  }

  public Topic getForAccount(Account.Id accountId) throws IOException {
    return get(topicNameFactory.createForAccount(accountId));
  }

  public Topic get(TopicName name) throws IOException {
    try (TopicAdminClient topicAdminClient = getTopicAdminClient()) {
      return topicAdminClient.getTopic(name);
    }
  }

  @VisibleForTesting
  public TopicAdminClient getTopicAdminClient() throws IOException {
    return TopicAdminClient.create(topicAdminSettings);
  }
}
