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

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubSubEventSubscriberTest {

  @Mock PubSubConfiguration confMock;
  @Mock SubscriberProvider subscriberProviderMock;
  @Mock PubSubSubscriberMetrics pubSubSubscriberMetricsMock;
  @Mock AckReplyConsumer ackReplyConsumerMock;

  private static final String TOPIC = "foo";
  private static final EventMessage eventMessage =
      new EventMessage(
          new EventMessage.Header(UUID.randomUUID(), UUID.randomUUID()), new ProjectCreatedEvent());
  private Gson gson = new EventGsonProvider().get();

  @Test
  public void shouldIncrementFailedToConsumeMessageWhenReceivingFails() {
    Consumer<EventMessage> failingConsumer =
        (message) -> {
          throw new RuntimeException("Error receiving message");
        };

    messageReceiver(failingConsumer)
        .receiveMessage(PubsubMessage.getDefaultInstance(), ackReplyConsumerMock);

    verify(pubSubSubscriberMetricsMock, only()).incrementFailedToConsumeMessage();
  }

  @Test
  public void shouldIncrementSucceedToConsumeMessageWhenReceivingSucceeds() {
    Consumer<EventMessage> succeedingConsumer = (message) -> {};

    PubsubMessage pubsubMessage = sampleMessage();

    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(pubSubSubscriberMetricsMock, only()).incrementSucceedToConsumeMessage();
  }

  private PubsubMessage sampleMessage() {
    String eventPayload = gson.toJson(eventMessage);
    ByteString data = ByteString.copyFromUtf8(eventPayload);
    return PubsubMessage.newBuilder().setData(data).build();
  }

  private MessageReceiver messageReceiver(Consumer<EventMessage> consumer) {
    return new PubSubEventSubscriber(
            gson, subscriberProviderMock, confMock, pubSubSubscriberMetricsMock, TOPIC, consumer)
        .getMessageReceiver();
  }
}
