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
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;
import org.apache.commons.lang3.RandomStringUtils;

@Singleton
public class PubSubUserSubNameFactory {
  private static final String SUB_NAME_PREFIX = "stream-events-";

  private final String serverId;
  private final String gcpProjectId;

  @Inject
  public PubSubUserSubNameFactory(@GerritServerId String serverId, PubSubConfiguration config) {
    this.serverId = serverId;
    this.gcpProjectId = config.getGCloudProject();
  }

  public ProjectSubscriptionName createForAccount(Account.Id accountId) {
    StringBuilder subName = new StringBuilder();
    subName.append(SUB_NAME_PREFIX);
    subName.append(serverId);
    subName.append("-");
    subName.append(accountId.get());
    subName.append("-");
    subName.append(RandomStringUtils.random(6, true, true));
    return ProjectSubscriptionName.of(gcpProjectId, subName.toString());
  }
}
