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

package com.googlesource.gerrit.plugins.pubsub.rest;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubRegistrationHandle;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserEventListenerHandlers;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserScopedEventListener;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserTopicNameFactory;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

@Singleton
@RequiresCapability(SubscribePubSubStreamEventsCapability.ID)
public class PutTopic extends PubSubRestModifyView {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final TopicAdminSettings topicAdminSettings;
  private final PubSubUserTopicNameFactory topicNameFactory;
  private final ConcurrentMap<Account.Id, PubSubRegistrationHandle>
      pubSubUserStreamEventListenerHandlers;
  private final PubSubUserScopedEventListener.Factory userScopedEventListenerFactory;

  @Inject
  public PutTopic(
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend,
      CredentialsProvider credentials,
      PubSubUserTopicNameFactory topicNameFactory,
      @PubSubUserEventListenerHandlers
          ConcurrentMap<Account.Id, PubSubRegistrationHandle> pubSubUserStreamEventListenerHandlers,
      PubSubUserScopedEventListener.Factory userScopedEventListenerFactory)
      throws IOException {
    super(userProvider, permissionBackend);
    this.topicAdminSettings =
        TopicAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
    this.topicNameFactory = topicNameFactory;
    this.pubSubUserStreamEventListenerHandlers = pubSubUserStreamEventListenerHandlers;
    this.userScopedEventListenerFactory = userScopedEventListenerFactory;
  }

  @Override
  Response<?> applyImpl(AccountResource rsrc, Input input) throws IOException {
    TopicName topicName = topicNameFactory.createForAccount(rsrc.getUser().getAccountId());
    try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
      Topic topic = topicAdminClient.createTopic(topicName);
      logger.atInfo().log("Created pubsub topic: %s", topic.getName());
    } catch (AlreadyExistsException e) {
    }
    if (!pubSubUserStreamEventListenerHandlers.containsKey(rsrc.getUser().getAccountId())) {
      userScopedEventListenerFactory.create(rsrc.getUser());
    }

    return Response.created();
  }
}
