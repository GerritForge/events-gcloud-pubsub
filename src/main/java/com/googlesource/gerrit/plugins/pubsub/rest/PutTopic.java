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

import com.google.api.gax.rpc.AlreadyExistsException;
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
import com.googlesource.gerrit.plugins.pubsub.TopicProvider;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubRegistrationHandle;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserEventListenerHandlers;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserScopedEventListener;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

@Singleton
@RequiresCapability(SubscribePubSubStreamEventsCapability.ID)
public class PutTopic extends PubSubRestModifyView<Input> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final TopicProvider topicProvider;
  private final ConcurrentMap<Account.Id, PubSubRegistrationHandle>
      pubSubUserStreamEventListenerHandlers;
  private final PubSubUserScopedEventListener.Factory userScopedEventListenerFactory;

  @Inject
  public PutTopic(
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend,
      TopicProvider topicProvider,
      @PubSubUserEventListenerHandlers
          ConcurrentMap<Account.Id, PubSubRegistrationHandle> pubSubUserStreamEventListenerHandlers,
      PubSubUserScopedEventListener.Factory userScopedEventListenerFactory) {
    super(userProvider, permissionBackend);
    this.topicProvider = topicProvider;
    this.pubSubUserStreamEventListenerHandlers = pubSubUserStreamEventListenerHandlers;
    this.userScopedEventListenerFactory = userScopedEventListenerFactory;
  }

  @Override
  Response<?> applyImpl(AccountResource rsrc, Input input) throws IOException {
    try {
      Topic topic = topicProvider.createForAccount(rsrc.getUser().getAccountId());
      logger.atInfo().log("Created pubsub topic: %s", topic.getName());
    } catch (AlreadyExistsException e) {
    }
    if (!pubSubUserStreamEventListenerHandlers.containsKey(rsrc.getUser().getAccountId())) {
      userScopedEventListenerFactory.create(rsrc.getUser());
    }

    return Response.created();
  }
}
