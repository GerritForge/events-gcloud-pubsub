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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.refEq;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.AckAwareConsumer;
import com.gerritforge.gerrit.eventbroker.EventDeserializer;
import com.gerritforge.gerrit.eventbroker.MessageAcknowledgement;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventGsonProvider;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
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
  @Mock OneOffRequestContext oneOffRequestContext;
  @Mock ManualRequestContext manualRequestContext;
  @Mock AckReplyConsumer ackReplyConsumerMock;
  @Mock AckAwareConsumer<Event> succeedingConsumer;
  @Captor ArgumentCaptor<Event> eventMessageCaptor;
  @Captor ArgumentCaptor<MessageAcknowledgement<Event>> acknowledgementCaptor;

  private static final String TOPIC = "foo";
  private static final String GROUP_ID = "bar";

  private Gson gson = new EventGsonProvider().get();
  private EventDeserializer deserializer = new EventDeserializer(gson);

  @Before
  public void setUp() {
    when(oneOffRequestContext.open()).thenReturn(manualRequestContext);
    when(confMock.isAutoCommitEnabled()).thenReturn(true);
  }

  @Test
  public void shouldIncrementFailedToConsumeMessageWhenReceivingFails() {
    AckAwareConsumer<Event> failingConsumer =
        (message, msgAck) -> {
          throw new RuntimeException("Error receiving message");
        };

    messageReceiver(failingConsumer)
        .receiveMessage(PubsubMessage.getDefaultInstance(), ackReplyConsumerMock);

    verify(pubSubSubscriberMetricsMock, only()).incrementFailedToConsumeMessage();
  }

  @Test
  public void shouldIncrementSucceedToConsumeMessageWhenReceivingSucceeds() {
    String instanceId = "instance-id";
    Event eventMessage = new ProjectCreatedEvent();
    eventMessage.instanceId = instanceId;
    PubsubMessage pubsubMessage = sampleMessage(eventMessage);

    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(pubSubSubscriberMetricsMock, only()).incrementSucceedToConsumeMessage();
  }

  @Test
  public void shouldConsumeEventWithoutSourceInstanceId() {
    Event eventWithoutSourceInstanceId = new ProjectCreatedEvent();
    PubsubMessage pubsubMessage = sampleMessage(eventWithoutSourceInstanceId);

    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(succeedingConsumer, times(1)).accept(refEq(eventWithoutSourceInstanceId), any());
  }

  @Test
  public void shouldParseEventObject() {
    String instanceId = "instance-id";
    Event event = new ProjectCreatedEvent();
    event.instanceId = instanceId;
    PubsubMessage pubsubMessage = sampleMessage(event);
    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(succeedingConsumer, only()).accept(eventMessageCaptor.capture(), any());
    Event result = eventMessageCaptor.getValue();
    assertThat(result.instanceId).isEqualTo(instanceId);
  }

  @Test
  public void shouldParseEventObjectWithHeaderAndBodyProjectName() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";
    event.projectName = "header_body_parser_project";
    PubsubMessage pubsubMessage = sampleMessage(event);
    messageReceiver(succeedingConsumer).receiveMessage(pubsubMessage, ackReplyConsumerMock);

    verify(succeedingConsumer, only()).accept(any(Event.class), any());
  }

  @Test
  public void shouldNotAcknowledgeAutomaticallyWhenManualAcknowledgementIsEnabled() {
    when(confMock.isAutoCommitEnabled()).thenReturn(false);
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";

    messageReceiver(succeedingConsumer).receiveMessage(sampleMessage(event), ackReplyConsumerMock);

    verify(ackReplyConsumerMock, never()).ack();
  }

  @Test
  public void shouldAcknowledgeMessageWhenManualAcknowledgementIsCalled() {
    when(confMock.isAutoCommitEnabled()).thenReturn(false);
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";

    messageReceiver(succeedingConsumer).receiveMessage(sampleMessage(event), ackReplyConsumerMock);

    MessageAcknowledgement<Event> acknowledgement = capturedAcknowledgement();
    Event receivedEvent = capturedEvent();
    acknowledgement.ack(receivedEvent);

    verify(ackReplyConsumerMock, only()).ack();
  }

  @Test
  public void shouldRejectExplicitAckWhenAutoCommitIsEnabled() {
    ProjectCreatedEvent event = new ProjectCreatedEvent();
    event.instanceId = "instance-id";

    messageReceiver(succeedingConsumer).receiveMessage(sampleMessage(event), ackReplyConsumerMock);

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class, () -> capturedAcknowledgement().ack(capturedEvent()));

    assertThat(thrown).hasMessageThat().contains("already acknowledged automatically");
    verify(ackReplyConsumerMock, only()).ack();
  }

  private PubsubMessage sampleMessage(Event event) {
    String eventPayload = gson.toJson(event);
    ByteString data = ByteString.copyFromUtf8(eventPayload);
    return PubsubMessage.newBuilder().setData(data).build();
  }

  private MessageReceiver messageReceiver(AckAwareConsumer<Event> consumer) {
    return new PubSubEventSubscriber(
            deserializer,
            subscriberProviderMock,
            confMock,
            pubSubSubscriberMetricsMock,
            oneOffRequestContext,
            TOPIC,
            GROUP_ID,
            consumer)
        .getMessageReceiver();
  }

  private Event capturedEvent() {
    verify(succeedingConsumer).accept(eventMessageCaptor.capture(), any());
    return eventMessageCaptor.getValue();
  }

  private MessageAcknowledgement<Event> capturedAcknowledgement() {
    verify(succeedingConsumer).accept(any(), acknowledgementCaptor.capture());
    return acknowledgementCaptor.getValue();
  }
}
