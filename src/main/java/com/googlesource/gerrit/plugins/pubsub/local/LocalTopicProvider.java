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
import com.googlesource.gerrit.plugins.pubsub.TopicProvider;
import com.googlesource.gerrit.plugins.pubsub.user.PubSubUserTopicNameFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;

public class LocalTopicProvider extends TopicProvider {

  @Inject
  public LocalTopicProvider(
      EnvironmentChecker environmentChecker, PubSubUserTopicNameFactory topicNameFactory)
      throws IOException {
    super(
        createChannelProvider(environmentChecker),
        NoCredentialsProvider.create(),
        topicNameFactory);
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
