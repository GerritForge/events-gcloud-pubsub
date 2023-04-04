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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.entities.Account;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.ExpirationPolicy;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.PushConfig.OidcToken;
import com.google.pubsub.v1.RetryPolicy;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;
import java.io.IOException;
import java.util.Optional;

@Singleton
public class PubSubUserSubscriptionProvider {
  private static final int RETENTION_SECONDS = 86400; // 1 day

  private final PubSubConfiguration pubSubProperties;
  private final SubscriptionAdminSettings settings;
  private final PubSubUserSubNameFactory subNameFactory;
  private final PushSubscriptionEndpointUrlBuilder endpointBuilder;

  @Inject
  public PubSubUserSubscriptionProvider(
      PubSubConfiguration pubSubProperties,
      CredentialsProvider credentials,
      PubSubUserSubNameFactory subNameFactory,
      PushSubscriptionEndpointUrlBuilder endpointBuilder)
      throws IOException {
    this.pubSubProperties = pubSubProperties;
    this.settings =
        SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
    this.subNameFactory = subNameFactory;
    this.endpointBuilder = endpointBuilder;
  }

  public PubSubUserSubscriptionProvider(
      TransportChannelProvider channelProvider,
      PubSubConfiguration pubSubProperties,
      CredentialsProvider credentials,
      PubSubUserSubNameFactory subNameFactory,
      PushSubscriptionEndpointUrlBuilder endpointBuilder)
      throws IOException {
    this.pubSubProperties = pubSubProperties;
    this.settings =
        SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentials)
            .build();
    this.subNameFactory = subNameFactory;
    this.endpointBuilder = endpointBuilder;
  }

  public Subscription getOrCreate(
      String topicId,
      CurrentUser user,
      String pushEndpoint,
      String verificationToken,
      boolean internal)
      throws IOException {
    try (SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(settings)) {
      String pushEndpointWithParameters;
      String audience;
      if (internal) {
        pushEndpointWithParameters = endpointBuilder.buildProxied(pushEndpoint, verificationToken);
        audience = pubSubProperties.getUserSubProxyEndpoint();
      } else {
        pushEndpointWithParameters = endpointBuilder.build(pushEndpoint, verificationToken);
        audience = pushEndpoint;
      }
      return getSubscription(subscriptionAdminClient, user.getAccountId())
          .orElseGet(
              () ->
                  subscriptionAdminClient.createSubscription(
                      createSubscriptionRequest(
                          user, topicId, pushEndpointWithParameters, audience)));
    }
  }

  private Subscription createSubscriptionRequest(
      CurrentUser user, String topicId, String pushEndpoint, String audience) {
    OidcToken token =
        OidcToken.newBuilder()
            .setServiceAccountEmail(pubSubProperties.getServiceAccountForUserSubs())
            .setAudience(audience)
            .build();
    PushConfig pushConfig =
        PushConfig.newBuilder().setPushEndpoint(pushEndpoint).setOidcToken(token).build();

    return Subscription.newBuilder()
        .setName(subNameFactory.createForAccount(user.getAccountId()).toString())
        .setPushConfig(pushConfig)
        .setTopic(TopicName.of(pubSubProperties.getGCloudProject(), topicId).toString())
        .setAckDeadlineSeconds(pubSubProperties.getAckDeadlineSeconds())
        .setRetainAckedMessages(false)
        .setMessageRetentionDuration(Duration.newBuilder().setSeconds(RETENTION_SECONDS).build())
        .setExpirationPolicy(
            ExpirationPolicy.newBuilder()
                .setTtl(Duration.newBuilder().setSeconds(RETENTION_SECONDS).build())
                .build())
        .setRetryPolicy(
            RetryPolicy.newBuilder()
                .setMinimumBackoff(Duration.newBuilder().setSeconds(10).build())
                .setMaximumBackoff(Duration.newBuilder().setSeconds(300).build()))
        .build();
  }

  @VisibleForTesting
  Optional<Subscription> getSubscription(String subName) throws IOException {
    try (SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(settings)) {
      return Optional.of(subscriptionAdminClient.getSubscription(subName));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  private Optional<Subscription> getSubscription(
      SubscriptionAdminClient subscriptionAdminClient, Account.Id accountId) {
    try {
      return Optional.of(
          subscriptionAdminClient.getSubscription(subNameFactory.createForAccount(accountId)));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }
}
