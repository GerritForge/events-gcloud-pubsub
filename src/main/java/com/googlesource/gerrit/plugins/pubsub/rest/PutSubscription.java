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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.pubsub.v1.Subscription;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserSubscriptionProvider;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserTopicNameFactory;
import java.io.IOException;

@Singleton
@RequiresCapability(SubscribePubSubStreamEventsCapability.ID)
public class PutSubscription extends PubSubRestModifyView<SubscriptionInput> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PubSubUserSubscriptionProvider subscriptionProvider;
  private final PubSubUserTopicNameFactory topicNameFactory;

  @Inject
  public PutSubscription(
      PubSubUserSubscriptionProvider subscriptionProvider,
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend,
      PubSubUserTopicNameFactory topicNameFactory) {
    super(userProvider, permissionBackend);
    this.subscriptionProvider = subscriptionProvider;
    this.topicNameFactory = topicNameFactory;
  }

  @Override
  public Response<SubscriptionInfo> applyImpl(AccountResource rsrc, SubscriptionInput input)
      throws RestApiException {
    if (input.pushEndpoint == null || input.pushEndpoint.isBlank()) {
      throw new BadRequestException("Missing pushEndpoint URL.");
    }
    Account.Id accountId = rsrc.getUser().getAccountId();
    try {
      Subscription sub =
          subscriptionProvider.getOrCreate(
              topicNameFactory.createForAccount(accountId).getTopic(),
              rsrc.getUser(),
              input.pushEndpoint,
              input.verificationToken,
              input.internal);
      return Response.created(SubscriptionInfo.create(sub, input.verificationToken));
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to create subscription for account %s", accountId);
      throw RestApiException.wrap("Failed to create subscription", e);
    }
  }
}
