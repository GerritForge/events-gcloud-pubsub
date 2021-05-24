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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import com.gerritforge.gerrit.eventbroker.EventDeserializer;
import com.gerritforge.gerrit.eventbroker.EventMessage;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubSubEventSubscriberTest {

  @Mock PubSubConfiguration confMock;
  @Mock SubscriberProvider subscriberProviderMock;
  @Mock PubSubSubscriberMetrics pubSubSubscriberMetricsMock;
  @Mock AckReplyConsumer ackReplyConsumerMock;
  @Mock Consumer<EventMessage> succeedingConsumer;
  @Captor ArgumentCaptor<EventMessage> eventMessageCaptor;

  private static final String TOPIC = "foo";
  private static final EventMessage eventMessage =
      new EventMessage(
          new EventMessage.Header(UUID.randomUUID(), UUID.randomUUID()), new ProjectCreatedEvent());
  private Gson gson = new EventGsonProvider().get();
  private EventDeserializer deserializer = new EventDeserializer(gson);

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
    PubsubMessage pubsubMessage = sampleMessage();

    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(pubSubSubscriberMetricsMock, only()).incrementSucceedToConsumeMessage();
  }

  @Test
  public void shouldSkipEventWithoutSourceInstanceId() {
    Event event = new ProjectCreatedEvent();
    EventMessage messageWithoutSourceInstanceId =
        new EventMessage(new EventMessage.Header(UUID.randomUUID(), (String) null), event);
    PubsubMessage pubsubMessage = sampleMessage(messageWithoutSourceInstanceId);

    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(succeedingConsumer, never()).accept(messageWithoutSourceInstanceId);
  }

  @Test
  public void shouldParseEventObject() {
    String instanceId = "instance-id";

    Event event = new ProjectCreatedEvent();
    event.instanceId = instanceId;
    PubsubMessage pubsubMessage = sampleMessage(event);
    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(succeedingConsumer, only()).accept(eventMessageCaptor.capture());
    EventMessage result = eventMessageCaptor.getValue();
    assertThat(result.getHeader().sourceInstanceId).isEqualTo(instanceId);
  }

  @Test
  public void shouldParseEventObjectWithHeaderAndBodyProjectName() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";
    event.projectName = "header_body_parser_project";
    PubsubMessage pubsubMessage = sampleMessage(event);
    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(succeedingConsumer, only()).accept(any(EventMessage.class));
  }

  private PubsubMessage sampleMessage(Event event) {
    String eventPayload = gson.toJson(event);
    ByteString data = ByteString.copyFromUtf8(eventPayload);
    return PubsubMessage.newBuilder().setData(data).build();
  }

  private PubsubMessage sampleMessage(EventMessage message) {
    String eventPayload = gson.toJson(message);
    ByteString data = ByteString.copyFromUtf8(eventPayload);
    return PubsubMessage.newBuilder().setData(data).build();
  }

  private PubsubMessage sampleMessage() {
    return sampleMessage(eventMessage);
  }

  private MessageReceiver messageReceiver(Consumer<EventMessage> consumer) {
    return new PubSubEventSubscriber(
            deserializer,
            subscriberProviderMock,
            confMock,
            pubSubSubscriberMetricsMock,
            TOPIC,
            consumer)
        .getMessageReceiver();
  }
}
