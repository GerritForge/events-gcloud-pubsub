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

import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.ACK_DEADLINE_SECONDS_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.DEFAULT_ACK_DEADLINE_SECONDS;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.DEFAULT_NUMBER_OF_SUBSCRIBERS;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.DEFAULT_SEND_STREAM_EVENTS;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.DEFAULT_SHUTDOWN_TIMEOUT;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.DEFAULT_STREAM_EVENTS_TOPIC;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.DEFAULT_SUBSCTIPRION_TIMEOUT;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.GCLOUD_PROJECT_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.NUMBER_OF_SUBSCRIBERS_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.PRIVATE_KEY_LOCATION_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.SEND_STREAM_EVENTS_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.SHUTDOWN_TIMEOUT_SECONDS_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.STREAM_EVENTS_TOPIC_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.SUBSCRIPTION_ID_FIELD;
import static com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration.SUBSCRIPTION_TIMEOUT_SECONDS_FIELD;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.Section;
import com.google.gerrit.server.config.GerritInstanceIdProvider;
import com.google.inject.Inject;

public class InitConfig implements InitStep {
  private final Section pluginSection;
  private final String pluginName;
  private final ConsoleUI ui;
  private final GerritInstanceIdProvider gerritInstanceIdProvider;

  @Inject
  InitConfig(
      Section.Factory sections,
      @PluginName String pluginName,
      GerritInstanceIdProvider gerritInstanceIdProvider,
      ConsoleUI ui) {
    this.pluginName = pluginName;
    this.ui = ui;
    this.gerritInstanceIdProvider = gerritInstanceIdProvider;
    this.pluginSection = sections.get("plugin", pluginName);
  }

  @Override
  public void run() throws Exception {
    ui.header(String.format("%s plugin", pluginName));

    boolean sendStreamEvents = ui.yesno(DEFAULT_SEND_STREAM_EVENTS, "Should send stream events?");
    pluginSection.set(SEND_STREAM_EVENTS_FIELD, Boolean.toString(sendStreamEvents));

    if (sendStreamEvents) {
      pluginSection.string(
          "Stream events topic", STREAM_EVENTS_TOPIC_FIELD, DEFAULT_STREAM_EVENTS_TOPIC);
    }

    pluginSection.string(
        "Number of subscribers", NUMBER_OF_SUBSCRIBERS_FIELD, DEFAULT_NUMBER_OF_SUBSCRIBERS);

    pluginSection.string(
        "Timeout for subscriber ACKs (secs)",
        ACK_DEADLINE_SECONDS_FIELD,
        DEFAULT_ACK_DEADLINE_SECONDS);

    pluginSection.string(
        "Timeout for subscriber connection (secs)",
        SUBSCRIPTION_TIMEOUT_SECONDS_FIELD,
        DEFAULT_SUBSCTIPRION_TIMEOUT);

    pluginSection.string(
        "Timeout for subscriber shutdown (secs)",
        SHUTDOWN_TIMEOUT_SECONDS_FIELD,
        DEFAULT_SHUTDOWN_TIMEOUT);

    mandatoryField(GCLOUD_PROJECT_FIELD, "Gcloud Project name", null);
    mandatoryField(SUBSCRIPTION_ID_FIELD, "Subscriber Id", gerritInstanceIdProvider.get());
    mandatoryField(PRIVATE_KEY_LOCATION_FIELD, "Private key location", null);
  }

  private void mandatoryField(String fieldName, String description, String dv) {
    String providedValue = pluginSection.string(description, fieldName, dv);

    while (Strings.isNullOrEmpty(providedValue) && !ui.isBatch()) {
      ui.message("'%s' is mandatory. Please specify a value.", fieldName);
      providedValue = pluginSection.string(description, fieldName, dv);
    }

    if (Strings.isNullOrEmpty(providedValue) && ui.isBatch()) {
      System.err.printf(
          "FATAL [%s plugin]: Could not set '%s' in batch mode. %s will not work%n",
          pluginName, fieldName, pluginName);
    }
  }
}
