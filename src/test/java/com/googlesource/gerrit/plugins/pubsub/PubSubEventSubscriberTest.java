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
import com.google.gerrit.json.OutputFormat;
import com.google.pubsub.v1.PubsubMessage;
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

    messageReceiver(succeedingConsumer)
        .receiveMessage(PubsubMessage.getDefaultInstance(), ackReplyConsumerMock);

    verify(pubSubSubscriberMetricsMock, only()).incrementSucceedToConsumeMessage();
  }

  private MessageReceiver messageReceiver(Consumer<EventMessage> consumer) {
    return new PubSubEventSubscriber(
            OutputFormat.JSON_COMPACT.newGson(),
            subscriberProviderMock,
            confMock,
            pubSubSubscriberMetricsMock,
            TOPIC,
            consumer)
        .getMessageReceiver();
  }
}
