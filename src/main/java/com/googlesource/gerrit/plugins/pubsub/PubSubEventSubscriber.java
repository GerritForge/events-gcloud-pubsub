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

import com.gerritforge.gerrit.eventbroker.EventDeserializer;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class PubSubEventSubscriber {

  public interface Factory {
    public PubSubEventSubscriber create(
        @Assisted("topic") String topic,
        @Assisted("groupId") String groupId,
        Consumer<Event> messageProcessor);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final EventDeserializer eventsDeserializer;
  private final PubSubSubscriberMetrics subscriberMetrics;
  private final OneOffRequestContext oneOffRequestContext;
  private final String topic;
  private final String groupId;
  private final Consumer<Event> messageProcessor;
  private final SubscriberProvider subscriberProvider;
  private final PubSubConfiguration config;
  private Subscriber subscriber;

  @Inject
  public PubSubEventSubscriber(
      EventDeserializer eventsDeserializer,
      SubscriberProvider subscriberProvider,
      PubSubConfiguration config,
      PubSubSubscriberMetrics subscriberMetrics,
      OneOffRequestContext oneOffRequestContext,
      @Assisted("topic") String topic,
      @Assisted("groupId") String groupId,
      @Assisted Consumer<Event> messageProcessor) {
    this.eventsDeserializer = eventsDeserializer;
    this.subscriberMetrics = subscriberMetrics;
    this.oneOffRequestContext = oneOffRequestContext;
    this.topic = topic;
    this.groupId = groupId;
    this.messageProcessor = messageProcessor;
    this.subscriberProvider = subscriberProvider;
    this.config = config;
  }

  public void subscribe() {
    try {
      subscriber = subscriberProvider.get(topic, groupId, getMessageReceiver());
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

  public String getGroupId() {
    return groupId;
  }

  public Consumer<Event> getMessageProcessor() {
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

  @VisibleForTesting
  MessageReceiver getMessageReceiver() {
    return (PubsubMessage message, AckReplyConsumer consumer) -> {
      try (ManualRequestContext ctx = oneOffRequestContext.open()) {
        Event event = eventsDeserializer.deserialize(message.getData().toStringUtf8());
        messageProcessor.accept(event);
        subscriberMetrics.incrementSucceedToConsumeMessage();
      } catch (Exception e) {
        logger.atSevere().withCause(e).log(
            "Exception when consuming message %s from topic %s [message: %s]",
            message.getMessageId(), topic, message.getData().toStringUtf8());
        subscriberMetrics.incrementFailedToConsumeMessage();
      } finally {
        consumer.ack();
      }
    };
  }
}
