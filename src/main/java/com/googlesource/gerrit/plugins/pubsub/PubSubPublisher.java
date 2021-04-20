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

import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PubSubPublisher {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public interface Factory {
    public PubSubPublisher create(String topic);
  }

  private final Gson gson;
  private final Publisher publisher;
  private final PubSubConfiguration pubSubProperties;

  @Inject
  public PubSubPublisher(
      PubSubConfiguration pubSubProperties,
      PublisherProvider publisherProvider,
      Gson gson,
      @Assisted String topic)
      throws IOException {
    this.gson = gson;
    this.publisher = publisherProvider.get(topic);
    this.pubSubProperties = pubSubProperties;
  }

  public boolean publish(Event event) {
    return publish(gson.toJson(event));
  }

  public boolean publish(EventMessage event) {
    return publish(gson.toJson(event));
  }

  private boolean publish(String eventPayload) {
    ByteString data = ByteString.copyFromUtf8(eventPayload);
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
    if (pubSubProperties.isSendAsync()) {
      return publishAsync(pubsubMessage) != null;
    }

    publishSync(pubsubMessage);
    return true;
  }

  private ApiFuture<String> publishAsync(PubsubMessage pubsubMessage) {
    return publisher.publish(pubsubMessage);
  }

  private void publishSync(PubsubMessage pubsubMessage) {
    try {
      ApiFuture<String> messageIdFuture = publishAsync(pubsubMessage);
      messageIdFuture.get(1000, TimeUnit.SECONDS);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      logger.atSevere().withCause(e).log("Cannot send the message");
    }
  }

  public void close() throws InterruptedException {
    if (publisher != null) {
      publisher.shutdown();
      publisher.awaitTermination(1, TimeUnit.MINUTES);
    }
  }
}
