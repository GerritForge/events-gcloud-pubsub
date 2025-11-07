// Copyright (C) 2025 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.pubsub;

import com.gerritforge.gerrit.eventbroker.BrokerApi;
import com.gerritforge.gerrit.eventbroker.TopicSubscriber;
import com.gerritforge.gerrit.eventbroker.TopicSubscriberWithGroupId;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class PubSubBrokerApi implements BrokerApi {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private PubSubConfiguration configuration;
  private PubSubPublisher.Factory publisherFactory;
  private PubSubEventSubscriber.Factory subscriberFactory;
  private Map<String, PubSubPublisher> publishers = new ConcurrentHashMap<>();
  private Set<PubSubEventSubscriber> subscribers;

  @Inject
  public PubSubBrokerApi(
      PubSubConfiguration configuration,
      PubSubPublisher.Factory publisherFactory,
      PubSubEventSubscriber.Factory subscriberFactory) {
    this.configuration = configuration;
    this.publisherFactory = publisherFactory;
    this.subscriberFactory = subscriberFactory;
    subscribers = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  @Override
  public ListenableFuture<Boolean> send(String topic, Event message) {
    return publishers.computeIfAbsent(topic, t -> publisherFactory.create(t)).publish(message);
  }

  @Override
  public void receiveAsync(String topic, Consumer<Event> eventConsumer) {
    receiveAsync(topic, null, eventConsumer);
  }

  @Override
  public void receiveAsync(String topic, @Nullable String maybeGroupId, Consumer<Event> consumer) {
    String groupId = Optional.ofNullable(maybeGroupId).orElse(configuration.getSubscriptionId());
    PubSubEventSubscriber subscriber = subscriberFactory.create(topic, groupId, consumer);
    subscribers.add(subscriber);
    subscriber.subscribe();
  }

  @Override
  public Set<TopicSubscriberWithGroupId> topicSubscribersWithGroupId() {
    return subscribers.stream()
        .map(
            s ->
                TopicSubscriberWithGroupId.topicSubscriberWithGroupId(
                    s.getGroupId(),
                    TopicSubscriber.topicSubscriber(s.getTopic(), s.getMessageProcessor())))
        .collect(Collectors.toSet());
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
  public void disconnect(String topic, String groupId) {
    subscribers.stream()
        .filter(s -> s.getGroupId().equals(groupId) && topic.equals(s.getTopic()))
        .forEach(
            c -> {
              subscribers.remove(c);
              c.shutdown();
            });
  }

  @Override
  public void replayAllEvents(String topic) {
    subscribers.stream()
        .filter(subscriber -> topic.equals(subscriber.getTopic()))
        .forEach(PubSubEventSubscriber::replayMessages);
  }
}
