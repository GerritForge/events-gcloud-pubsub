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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.metrics.Counter1;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PubSubPublisherMetrics extends PubSubEventsMetrics {
  private static final String PUBLISHER_SUCCESS_COUNTER = "publisher_success_counter";
  private static final String PUBLISHER_FAILURE_COUNTER = "publisher_failure_counter";

  private final Counter1<String> publisherSuccessCounter;
  private final Counter1<String> publisherFailureCounter;

  @Inject
  public PubSubPublisherMetrics(MetricMaker metricMaker, @PluginName String pluginName) {

    this.publisherSuccessCounter =
        metricMaker.newCounter(
            String.join("/", pluginName, PUBLISHER_SUCCESS_COUNTER),
            rateDescription("messages", "Number of successfully published messages"),
            stringField(PUBLISHER_SUCCESS_COUNTER, "Count of published messages"));
    this.publisherFailureCounter =
        metricMaker.newCounter(
            String.join("/", pluginName, PUBLISHER_FAILURE_COUNTER),
            rateDescription("errors", "Number of messages failed to publish"),
            stringField(PUBLISHER_FAILURE_COUNTER, "Count of messages failed to publish"));
  }

  public void incrementSucceedToPublishMessage() {
    publisherSuccessCounter.increment(PUBLISHER_SUCCESS_COUNTER);
  }

  public void incrementFailedToPublishMessage() {
    publisherFailureCounter.increment(PUBLISHER_FAILURE_COUNTER);
  }
}
