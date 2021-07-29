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

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGson;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PubSubPublisher {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public interface Factory {
    public PubSubPublisher create(String topic);
  }

  private final Gson gson;
  private final PubSubPublisherMetrics publisherMetrics;
  private final String topic;
  private final Publisher publisher;
  private final PubSubConfiguration pubSubProperties;

  @Inject
  public PubSubPublisher(
      PubSubConfiguration pubSubProperties,
      PublisherProvider publisherProvider,
      @EventGson Gson gson,
      PubSubPublisherMetrics publisherMetrics,
      @Assisted String topic)
      throws IOException {
    this.gson = gson;
    this.publisherMetrics = publisherMetrics;
    this.topic = topic;
    this.publisher = publisherProvider.get(topic);
    this.pubSubProperties = pubSubProperties;
  }

  public ListenableFuture<Boolean> publish(Event event) {
    return publish(gson.toJson(event));
  }

  private ListenableFuture<Boolean> publish(String eventPayload) {
    ByteString data = ByteString.copyFromUtf8(eventPayload);
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
    return publishAsync(pubsubMessage);
  }

  private ListenableFuture<Boolean> publishAsync(PubsubMessage pubsubMessage) {
    ApiFuture<String> publish = publisher.publish(pubsubMessage);
    ApiFutures.addCallback(
        publish,
        new ApiFutureCallback<String>() {
          @Override
          public void onFailure(Throwable t) {
            logger.atSevere().withCause(t).log(
                "Exception when publishing message (id:%s) to topic '%s' [message: %s]",
                pubsubMessage.getMessageId(), topic, pubsubMessage.getData().toStringUtf8());
            publisherMetrics.incrementFailedToPublishMessage();
          }

          @Override
          public void onSuccess(String messageId) {
            logger.atFine().log(
                "Successfully published message (id:%s) to topic '%s' [message: %s]",
                messageId, topic, pubsubMessage.getData().toStringUtf8());

            publisherMetrics.incrementSucceedToPublishMessage();
          }
        },
        MoreExecutors.directExecutor());

    return Futures.transform(
        JdkFutureAdapters.listenInPoolThread(publish),
        Objects::nonNull,
        MoreExecutors.directExecutor());
  }

  public void close() throws InterruptedException {
    if (publisher != null) {
      publisher.shutdown();
      publisher.awaitTermination(1, TimeUnit.MINUTES);
    }
  }
}
