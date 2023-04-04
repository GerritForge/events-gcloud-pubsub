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
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.apache.http.client.utils.URIBuilder;

@Singleton
public class PubSubUserSubscriptionProvider {
  private static final int RETENTION_SECONDS = 86400; // 1 day

  private final PubSubConfiguration pubSubProperties;
  private final SubscriptionAdminSettings settings;
  private final PubSubUserSubNameFactory subNameFactory;

  @Inject
  public PubSubUserSubscriptionProvider(
      PubSubConfiguration pubSubProperties,
      CredentialsProvider credentials,
      PubSubUserSubNameFactory subNameFactory)
      throws IOException {
    this.pubSubProperties = pubSubProperties;
    this.settings =
        SubscriptionAdminSettings.newBuilder().setCredentialsProvider(credentials).build();
    this.subNameFactory = subNameFactory;
  }

  public Subscription getOrCreate(
      String topicId,
      CurrentUser user,
      URI pushEndpoint,
      String verificationToken,
      boolean internal)
      throws IOException, URISyntaxException {
    try (SubscriptionAdminClient subscriptionAdminClient =
        SubscriptionAdminClient.create(settings)) {
      URI pushEndpointWithParameters;
      String audience;
      if (internal) {
        pushEndpointWithParameters = buildProxyPushEndpointURI(pushEndpoint, verificationToken);
        audience = String.format("https://%s", pubSubProperties.getUserSubProxyEndpoint());
      } else {
        pushEndpointWithParameters =
            new URIBuilder(pushEndpoint).addParameter("token", verificationToken).build();
        audience = user.getLoggableName();
      }
      return getSubscription(subscriptionAdminClient, user)
          .orElseGet(
              () ->
                  subscriptionAdminClient.createSubscription(
                      createSubscriptionRequest(
                          user, topicId, pushEndpointWithParameters, audience)));
    }
  }

  private URI buildProxyPushEndpointURI(URI pushEndpoint, String verificationToken)
      throws URISyntaxException {
    String proxyHost = pubSubProperties.getUserSubProxyEndpoint();
    if (proxyHost == null || proxyHost.isBlank()) {
      throw new IllegalArgumentException("Can't push to internal network. Proxu URL not set.");
    }
    return new URIBuilder()
        .setScheme("https")
        .setHost(proxyHost)
        .setParameter("host", pushEndpoint.getHost())
        .setParameter("path", pushEndpoint.getPath())
        .setParameter("token", verificationToken)
        .build();
  }

  private Subscription createSubscriptionRequest(
      CurrentUser user, String topicId, URI pushEndpoint, String audience) {
    OidcToken token =
        OidcToken.newBuilder()
            .setServiceAccountEmail(pubSubProperties.getServiceAccountForUserSubs())
            .setAudience(audience)
            .build();
    PushConfig pushConfig =
        PushConfig.newBuilder()
            .setPushEndpoint(pushEndpoint.toString())
            .setOidcToken(token)
            .build();

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

  private Optional<Subscription> getSubscription(
      SubscriptionAdminClient subscriptionAdminClient, CurrentUser user) {
    try {
      // we should use subscriptionAdminClient.listSubscriptions but for local setup this method
      // throws UNKNOWN_EXCEPTION
      return Optional.of(
          subscriptionAdminClient.getSubscription(
              subNameFactory.createForAccount(user.getAccountId())));
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }
}
