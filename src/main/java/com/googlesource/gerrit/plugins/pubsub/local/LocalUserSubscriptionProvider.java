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

package com.googlesource.gerrit.plugins.pubsub.local;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserSubNameFactory;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserSubscriptionProvider;
import com.googlesource.gerrit.plugins.pubsub.user.PushSubscriptionEndpointUrlBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;

@Singleton
public class LocalUserSubscriptionProvider extends PubSubUserSubscriptionProvider {

  @Inject
  public LocalUserSubscriptionProvider(
      EnvironmentChecker environmentChecker,
      PubSubConfiguration pubSubProperties,
      PubSubUserSubNameFactory subNameFactory,
      PushSubscriptionEndpointUrlBuilder endpointBuilder)
      throws IOException {
    super(
        createChannelProvider(environmentChecker),
        pubSubProperties,
        NoCredentialsProvider.create(),
        subNameFactory,
        endpointBuilder);
  }

  public static TransportChannelProvider createChannelProvider(
      EnvironmentChecker environmentChecker) {
    ManagedChannel channel =
        ManagedChannelBuilder.forTarget(environmentChecker.getLocalHostAndPort().get())
            .usePlaintext()
            .build();
    TransportChannelProvider channelProvider =
        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    return channelProvider;
  }
}
