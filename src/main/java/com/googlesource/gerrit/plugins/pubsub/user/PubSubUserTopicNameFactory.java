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

import com.google.gerrit.entities.Account;
import com.google.gerrit.server.config.GerritServerId;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;
import java.util.Optional;

@Singleton
public class PubSubUserTopicNameFactory {
  private static final String TOPIC_NAME_PREFIX = "stream-events-";

  private final String gcpProjectId;
  private final String serverId;

  @Inject
  public PubSubUserTopicNameFactory(@GerritServerId String serverId, PubSubConfiguration config) {
    this.serverId = serverId;
    this.gcpProjectId = config.getGCloudProject();
  }

  public TopicName createForAccount(Account.Id accountId) {
    return TopicName.of(gcpProjectId, TOPIC_NAME_PREFIX + serverId + "-" + accountId.get());
  }

  public Optional<Account.Id> getAccountId(String topicName) {
    if (topicName.startsWith(TOPIC_NAME_PREFIX)) {
      return Account.Id.tryParse(topicName.substring(TOPIC_NAME_PREFIX.length()));
    }
    return Optional.empty();
  }
}
