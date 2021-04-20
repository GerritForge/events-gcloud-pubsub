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

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.gerritforge.gerrit.eventbroker.TopicSubscriber;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class PubSubBrokerApi implements BrokerApi {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private PubSubPublisher.Factory publisherFactory;
  private PubSubEventSubscriber.Factory subscriberFactory;
  private Map<String, PubSubPublisher> publishers = new ConcurrentHashMap<>();
  private Set<PubSubEventSubscriber> subscribers;

  @Inject
  public PubSubBrokerApi(
      PubSubPublisher.Factory publisherFactory, PubSubEventSubscriber.Factory subscriberFactory) {
    this.publisherFactory = publisherFactory;
    this.subscriberFactory = subscriberFactory;
    subscribers = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  @Override
  public boolean send(String topic, EventMessage message) {
    return publishers.computeIfAbsent(topic, t -> publisherFactory.create(t)).publish(message);
  }

  @Override
  public void receiveAsync(String topic, Consumer<EventMessage> eventConsumer) {
    PubSubEventSubscriber subscriber = subscriberFactory.create(topic, eventConsumer);
    subscribers.add(subscriber);
    subscriber.subscribe();
  }

  @Override
  public Set<TopicSubscriber> topicSubscribers() {
    return subscribers.stream()
        .map(s -> TopicSubscriber.topicSubscriber(s.getTopic(), s.getMessageProcessor()))
        .collect(Collectors.toSet());
  }

  @Override
  public void disconnect() {
    publishers
        .values()
        .forEach(
            publisher -> {
              try {
                publisher.close();
              } catch (InterruptedException e) {
                logger.atSevere().withCause(e).log("Disconnect failed");
              }
            });

    for (PubSubEventSubscriber subscriber : subscribers) {
      subscriber.shutdown();
    }
    subscribers.clear();
  }

  @Override
  public void replayAllEvents(String topic) {
    subscribers.stream()
        .filter(subscriber -> topic.equals(subscriber.getTopic()))
        .forEach(PubSubEventSubscriber::replayMessages);
  }
}
