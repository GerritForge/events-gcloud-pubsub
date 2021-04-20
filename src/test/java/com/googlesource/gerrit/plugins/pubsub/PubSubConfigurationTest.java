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
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubSubConfigurationTest {
  private static final String PLUGIN_NAME = "gcloud-pubsub-events";
  private static final String subscriptionId = "some-subscription-id";
  private static final String gerritInstanceId = "some-gerrit-id";
  private static final String gCloudProject = "gcloud-test-project";
  private static final String privateKeyLocation = "/some/path";

  private PluginConfig.Update pluginConfig;
  @Mock private PluginConfigFactory pluginConfigFactoryMock;

  @Before
  public void setup() {
    pluginConfig = PluginConfig.Update.forTest(PLUGIN_NAME, new Config());
    pluginConfig.setString("subscriptionId", subscriptionId);
    pluginConfig.setString("gcloudProject", gCloudProject);
    pluginConfig.setString("privateKeyLocation", privateKeyLocation);
  }

  @Test
  public void shouldUseSubscriptionIdWhenConfiguredEvenIfGerritInstanceIdIsNull() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    PubSubConfiguration configuration =
        new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, null);

    assertThat(configuration.getSubscriptionId()).isEqualTo(subscriptionId);
  }

  @Test
  public void shouldUseSubscriptionIdWhenConfiguredEvenIfGerritInstanceIdIsDefined() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    PubSubConfiguration configuration =
        new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, gerritInstanceId);

    assertThat(configuration.getSubscriptionId()).isEqualTo(subscriptionId);
  }

  @Test
  public void shouldUseGerritInstanceIdWhenSubscriptionIdIsEmpty() {
    pluginConfig.setString("subscriptionId", "");
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    PubSubConfiguration configuration =
        new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, gerritInstanceId);

    assertThat(configuration.getSubscriptionId()).isEqualTo(gerritInstanceId);
  }

  @Test
  public void shouldThrowExceptionWhenSubscriptionIdIsNotDefinedAndGerritInstanceIdIsNull() {
    pluginConfig.setString("subscriptionId", "");
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () -> new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, null));

    assertThat(thrown).hasMessageThat().contains("parameter 'subscriptionId' is mandatory");
  }

  @Test
  public void shouldReadGCloudProjectWhenConfigured() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    PubSubConfiguration configuration =
        new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, gerritInstanceId);

    assertThat(configuration.getGCloudProject()).isEqualTo(gCloudProject);
  }

  @Test
  public void shouldThrowExceptionWhenGCloudProjectIsNotDefined() {
    pluginConfig.setString("gcloudProject", "");
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () -> new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, gerritInstanceId));

    assertThat(thrown).hasMessageThat().contains("parameter 'gcloudProject' is mandatory");
  }

  @Test
  public void shouldReadPrivateKeyLocationWhenConfigured() {
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    PubSubConfiguration configuration =
        new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, gerritInstanceId);

    assertThat(configuration.getPrivateKeyLocation()).isEqualTo(privateKeyLocation);
  }

  @Test
  public void shouldThrowExceptionWhenPrivateKeyLocationIsNotDefined() {
    pluginConfig.setString("privateKeyLocation", "");
    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    IllegalStateException thrown =
        assertThrows(
            IllegalStateException.class,
            () -> new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, gerritInstanceId));

    assertThat(thrown).hasMessageThat().contains("parameter 'privateKeyLocation' is mandatory");
  }
}
