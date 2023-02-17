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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PubSubConfiguration {
  static final String GCLOUD_PROJECT_FIELD = "gcloudProject";
  static final String SUBSCRIPTION_ID_FIELD = "subscriptionId";
  static final String PRIVATE_KEY_LOCATION_FIELD = "privateKeyLocation";
  static final String STREAM_EVENTS_TOPIC_FIELD = "streamEventsTopic";
  static final String SEND_STREAM_EVENTS_FIELD = "sendStreamEvents";
  static final String NUMBER_OF_SUBSCRIBERS_FIELD = "numberOfSubscribers";
  static final String ACK_DEADLINE_SECONDS_FIELD = "ackDeadlineSeconds";
  static final String SUBSCRIPTION_TIMEOUT_SECONDS_FIELD = "subscribtionTimeoutInSeconds";
  static final String SHUTDOWN_TIMEOUT_SECONDS_FIELD = "shutdownTimeoutInSeconds";
  static final String RETAIN_ACKED_MESSAGES_FIELD = "retainAckedMessages";
  static final String ENABLE_USER_STREAM_EVENTS_FIELD = "enableUserStreamEvents";

  static final String DEFAULT_NUMBER_OF_SUBSCRIBERS = "6";
  static final String DEFAULT_ACK_DEADLINE_SECONDS = "10";
  static final String DEFAULT_SUBSCTIPRION_TIMEOUT = "10";
  static final String DEFAULT_SHUTDOWN_TIMEOUT = "10";
  static final String DEFAULT_STREAM_EVENTS_TOPIC = "gerrit";
  static final boolean DEFAULT_SEND_STREAM_EVENTS = false;
  static final boolean DEFAULT_RETAIN_ACKED_MESSAGES = true;
  static final boolean DEFAULT_ENABLE_USER_STREAM_EVENTS = false;

  private final String gcloudProject;
  private final String subscriptionId;
  private final Integer numberOfSubscribers;
  private final String privateKeyLocation;
  private final Integer ackDeadlineSeconds;
  private final Long subscribtionTimeoutInSeconds;
  private final Long shutdownTimeoutInSeconds;
  private final String streamEventsTopic;
  private final PluginConfig fromGerritConfig;
  private final boolean sendStreamEvents;
  private final boolean retainAckedMessages;
  private final boolean enableUserStreamEvents;

  @Inject
  public PubSubConfiguration(
      PluginConfigFactory configFactory,
      @PluginName String pluginName,
      @Nullable @GerritInstanceId String instanceId) {
    this.fromGerritConfig = configFactory.getFromGerritConfig(pluginName);
    this.gcloudProject = getMandatoryString(GCLOUD_PROJECT_FIELD);
    this.subscriptionId = getMandatoryString(SUBSCRIPTION_ID_FIELD, instanceId);
    this.privateKeyLocation = getMandatoryString(PRIVATE_KEY_LOCATION_FIELD);
    this.streamEventsTopic =
        fromGerritConfig.getString(STREAM_EVENTS_TOPIC_FIELD, DEFAULT_STREAM_EVENTS_TOPIC);
    this.sendStreamEvents =
        fromGerritConfig.getBoolean(SEND_STREAM_EVENTS_FIELD, DEFAULT_SEND_STREAM_EVENTS);
    this.numberOfSubscribers =
        Integer.parseInt(
            fromGerritConfig.getString(NUMBER_OF_SUBSCRIBERS_FIELD, DEFAULT_NUMBER_OF_SUBSCRIBERS));
    this.ackDeadlineSeconds =
        Integer.parseInt(
            fromGerritConfig.getString(ACK_DEADLINE_SECONDS_FIELD, DEFAULT_ACK_DEADLINE_SECONDS));
    this.subscribtionTimeoutInSeconds =
        Long.parseLong(
            fromGerritConfig.getString(
                SUBSCRIPTION_TIMEOUT_SECONDS_FIELD, DEFAULT_SUBSCTIPRION_TIMEOUT));
    this.shutdownTimeoutInSeconds =
        Long.parseLong(
            fromGerritConfig.getString(SHUTDOWN_TIMEOUT_SECONDS_FIELD, DEFAULT_SHUTDOWN_TIMEOUT));
    this.retainAckedMessages =
        fromGerritConfig.getBoolean(RETAIN_ACKED_MESSAGES_FIELD, DEFAULT_RETAIN_ACKED_MESSAGES);
    this.enableUserStreamEvents =
        fromGerritConfig.getBoolean(
            ENABLE_USER_STREAM_EVENTS_FIELD, DEFAULT_ENABLE_USER_STREAM_EVENTS);
  }

  public String getGCloudProject() {
    return gcloudProject;
  }

  public Integer getNumberOfSubscribers() {
    return numberOfSubscribers;
  }

  public String getPrivateKeyLocation() {
    return privateKeyLocation;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public Integer getAckDeadlineSeconds() {
    return ackDeadlineSeconds;
  }

  public Long getSubscribtionTimeoutInSeconds() {
    return subscribtionTimeoutInSeconds;
  }

  public Long getShutdownTimeoutInSeconds() {
    return shutdownTimeoutInSeconds;
  }

  public String getStreamEventsTopic() {
    return streamEventsTopic;
  }

  private String getMandatoryString(String name) throws IllegalStateException {
    return getMandatoryString(name, null);
  }

  private String getMandatoryString(String name, String defaultValue) throws IllegalStateException {
    String value = fromGerritConfig.getString(name, defaultValue);
    if (value == null) {
      throw new IllegalStateException(
          String.format("Invalid configuration: parameter '%s' is mandatory", name));
    }
    return value;
  }

  public boolean isSendStreamEvents() {
    return sendStreamEvents;
  }

  public boolean isRetainAckedMessages() {
    return retainAckedMessages;
  }

  public boolean isEnableUserStreamEvents() {
    return enableUserStreamEvents;
  }
}
