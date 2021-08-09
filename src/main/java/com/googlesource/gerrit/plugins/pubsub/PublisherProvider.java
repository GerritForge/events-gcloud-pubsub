// Copyright (C) 2021 The Android Open Source Project
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
import com.google.api.gax.core.ExecutorProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.TopicName;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;

@Singleton
public class PublisherProvider {

  protected CredentialsProvider credentials;
  protected PubSubConfiguration config;
  private final ExecutorProvider executorProvider;

  @Inject
  public PublisherProvider(
      CredentialsProvider credentials,
      PubSubConfiguration config,
      @PublisherExecutorProvider ExecutorProvider executorProvider) {
    this.credentials = credentials;
    this.config = config;
    this.executorProvider = executorProvider;
  }

  public Publisher get(String topic) throws IOException {
    return configure(Publisher.newBuilder(TopicName.of(config.getGCloudProject(), topic))).build();
  }

  protected Publisher.Builder configure(Publisher.Builder builder) {
    return builder
        .setExecutorProvider(executorProvider)
        .setCredentialsProvider(credentials)
        .setChannelProvider(
            PublisherStubSettings.defaultGrpcTransportProviderBuilder()
                .setExecutor(executorProvider.getExecutor())
                .setChannelConfigurator(
                    managedChannelBuilder -> {
                      NettyChannelBuilder nettyChannelBuilder =
                          (NettyChannelBuilder)
                              managedChannelBuilder.executor(executorProvider.getExecutor());
                      nettyChannelBuilder.eventLoopGroup(
                          new NioEventLoopGroup(1, executorProvider.getExecutor()));
                      nettyChannelBuilder.channelType(NioSocketChannel.class);
                      nettyChannelBuilder.usePlaintext();

                      return nettyChannelBuilder;
                    })
                .build());
  }
}
