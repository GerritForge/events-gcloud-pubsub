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
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.eventbroker.EventsBrokerConfiguration;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gerrit.json.OutputFormat;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubSubPublisherTest {
  PubSubPublisher objectUnderTest;

  @Mock PubSubConfiguration confMock;
  @Mock PublisherProvider publisherProviderMock;
  @Mock Publisher publisherMock;
  @Mock PubSubPublisherMetrics pubSubPublisherMetricsMock;
  @Mock EventsBrokerConfiguration eventsBrokerConfigurationMock;
  @Captor ArgumentCaptor<PubsubMessage> messageCaptor;

  private static final String TOPIC = "foo";
  private static final Event eventMessage = new ProjectCreatedEvent();

  @Before
  public void setUp() throws IOException {
    when(publisherProviderMock.get(TOPIC)).thenReturn(publisherMock);
    when(eventsBrokerConfigurationMock.getPartitionsForTopic(TOPIC)).thenReturn(List.of());
    objectUnderTest =
        new PubSubPublisher(
            confMock,
            publisherProviderMock,
            OutputFormat.JSON_COMPACT.newGson(),
            pubSubPublisherMetricsMock,
            eventsBrokerConfigurationMock,
            TOPIC);
  }

  @Test
  public void shouldIncrementFailedToPublishMessageWhenAsyncPublishFails() {
    when(publisherMock.publish(any()))
        .thenReturn(ApiFutures.immediateFailedFuture(new Exception("Something went wrong")));

    objectUnderTest.publish(eventMessage);

    verify(pubSubPublisherMetricsMock, only()).incrementFailedToPublishMessage();
  }

  @Test
  public void shouldIncrementSuccessToPublishMessageWhenAsyncPublishSucceeds() {
    publisherSucceeds();

    objectUnderTest.publish(eventMessage);

    verify(pubSubPublisherMetricsMock, only()).incrementSucceedToPublishMessage();
  }

  @Test
  public void shouldPublishWithoutPartitionAttributeWhenTopicHasNoPartitions() {
    publisherSucceeds();

    objectUnderTest.publish(eventMessage);

    verify(publisherMock).publish(messageCaptor.capture());
    assertThat(messageCaptor.getValue().getAttributesMap()).doesNotContainKey("type");
  }

  @Test
  public void shouldPublishLogicalPartitionAsMessageAttribute() {
    configurePartitions("type", eventMessage.getType());
    publisherSucceeds();

    objectUnderTest.publish(eventMessage);

    verify(publisherMock).publish(messageCaptor.capture());
    assertThat(messageCaptor.getValue().getAttributesMap())
        .containsEntry("type", eventMessage.getType());
  }

  @Test
  public void shouldRejectEventWithoutPartitionProperty() {
    configurePartitions("missingProperty", eventMessage.getType());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> objectUnderTest.publish(eventMessage));

    assertThat(exception).hasMessageThat().contains("missingProperty");
    verify(publisherMock, never()).publish(any());
  }

  @Test
  public void shouldRejectUnconfiguredPartitionValue() {
    configurePartitions("type", "some-other-event");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> objectUnderTest.publish(eventMessage));

    assertThat(exception).hasMessageThat().contains(eventMessage.getType());
    verify(publisherMock, never()).publish(any());
  }

  private void configurePartitions(String property, String... partitions) {
    when(eventsBrokerConfigurationMock.getPartitionsForTopic(TOPIC))
        .thenReturn(List.of(partitions));
    when(eventsBrokerConfigurationMock.getEventPropertyForTopic(TOPIC))
        .thenReturn(Optional.of(property));
  }

  private void publisherSucceeds() {
    when(publisherMock.publish(any())).thenReturn(ApiFutures.immediateFuture("some-message-id"));
  }
}
