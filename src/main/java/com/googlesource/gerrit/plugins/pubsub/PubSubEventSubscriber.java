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
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.flogger.FluentLogger;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class PubSubEventSubscriber {

  public interface Factory {
    public PubSubEventSubscriber create(String topic, Consumer<EventMessage> messageProcessor);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Gson gson;
  private final String topic;
  private final Consumer<EventMessage> messageProcessor;
  private final SubscriberProvider subscriberProvider;
  private final PubSubConfiguration config;
  private Subscriber subscriber;

  @Inject
  public PubSubEventSubscriber(
      Gson gson,
      SubscriberProvider subscriberProvider,
      PubSubConfiguration config,
      @Assisted String topic,
      @Assisted Consumer<EventMessage> messageProcessor) {
    this.gson = gson;
    this.topic = topic;
    this.messageProcessor = messageProcessor;
    this.subscriberProvider = subscriberProvider;
    this.config = config;
  }

  public void subscribe() {
    MessageReceiver receiver =
        (PubsubMessage message, AckReplyConsumer consumer) -> {
          EventMessage event = gson.fromJson(message.getData().toStringUtf8(), EventMessage.class);
          messageProcessor.accept(event);
          consumer.ack();
        };

    try {
      subscriber = subscriberProvider.get(topic, receiver);
      subscriber
          .startAsync()
          .awaitRunning(config.getSubscribtionTimeoutInSeconds(), TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      logger.atSevere().withCause(e).log("Timeout during subscribing to the topic %s", topic);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Exception during subscribing to the topic %s", topic);
    }
  }

  public String getTopic() {
    return topic;
  }

  public Consumer<EventMessage> getMessageProcessor() {
    return messageProcessor;
  }

  public void replayMessages() {
    subscriberProvider.replayMessages(subscriber.getSubscriptionNameString());
  }

  public void shutdown() {
    try {
      subscriber
          .stopAsync()
          .awaitTerminated(config.getShutdownTimeoutInSeconds(), TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      logger.atSevere().withCause(e).log("Timeout during subscriber shutdown");
    }
  }
}
