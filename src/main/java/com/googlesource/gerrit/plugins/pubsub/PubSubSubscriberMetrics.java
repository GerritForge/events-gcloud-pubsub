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

import com.google.gerrit.metrics.Counter1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class PubSubSubscriberMetrics extends PubSubEventsMetrics {

  private static final String SUBSCRIBER_SUCCESS_COUNTER =
      "subscriber_msg_consumer_success_counter";
  private static final String SUBSCRIBER_FAILURE_COUNTER =
      "subscriber_msg_consumer_failure_counter";

  private final Counter1<String> subscriberSuccessCounter;
  private final Counter1<String> subscriberFailureCounter;

  @Inject
  public PubSubSubscriberMetrics(MetricMaker metricMaker) {
    this.subscriberSuccessCounter =
        metricMaker.newCounter(
            "gcloud_pubsub/subscriber/subscriber_message_consumer_success_counter",
            new Description("Number of messages successfully consumed by the subscriber consumer")
                .setRate()
                .setUnit("messages"),
            stringField(SUBSCRIBER_SUCCESS_COUNTER, "Count of successfully consumed messages"));
    this.subscriberFailureCounter =
        metricMaker.newCounter(
            "gcloud_pubsub/subscriber/subscriber_message_consumer_failure_counter",
            new Description("Number of messages failed to consume by the subscriber consumer")
                .setRate()
                .setUnit("errors"),
            stringField(SUBSCRIBER_FAILURE_COUNTER, "Count of messages failed to consume"));
  }

  public void incrementFailedToConsumeMessage() {
    subscriberFailureCounter.increment(SUBSCRIBER_FAILURE_COUNTER);
  }

  public void incrementSucceedToConsumeMessage() {
    subscriberSuccessCounter.increment(SUBSCRIBER_SUCCESS_COUNTER);
  }
}
