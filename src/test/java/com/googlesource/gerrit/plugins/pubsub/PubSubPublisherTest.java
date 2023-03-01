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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gerrit.json.OutputFormat;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectCreatedEvent;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubSubPublisherTest {
  PubSubPublisher objectUnderTest;

  @Mock PubSubConfiguration confMock;
  @Mock PublisherProvider publisherProviderMock;
  @Mock Publisher publisherMock;
  @Mock PubSubPublisherMetrics.Factory pubSubPublisherMetricsMockFactory;
  @Mock PubSubPublisherMetrics pubSubPublisherMetricsMock;

  private static final String PROJECT = "bar";
  private static final String TOPIC = "foo";
  private static final Event eventMessage = new ProjectCreatedEvent();

  @Before
  public void setUp() throws IOException {
    when(confMock.getGCloudProject()).thenReturn(PROJECT);
    when(pubSubPublisherMetricsMockFactory.create(TopicName.of(PROJECT, TOPIC)))
        .thenReturn(pubSubPublisherMetricsMock);
    when(publisherProviderMock.get(TOPIC)).thenReturn(publisherMock);
    objectUnderTest =
        new PubSubPublisher(
            confMock,
            publisherProviderMock,
            OutputFormat.JSON_COMPACT.newGson(),
            pubSubPublisherMetricsMockFactory,
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
    when(publisherMock.publish(any())).thenReturn(ApiFutures.immediateFuture("some-message-id"));

    objectUnderTest.publish(eventMessage);

    verify(pubSubPublisherMetricsMock, only()).incrementSucceedToPublishMessage();
  }
}
