// Copyright (C) 2023 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.pubsub.user;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PushSubscriptionEndpointBuilderTest {
  private static final String PLUGIN_NAME = "events-gcloud-pubsub";

  private PushSubscriptionEndpointUrlBuilder endpointBuilder;
  private PluginConfig.Update pluginConfig;
  @Mock private PluginConfigFactory pluginConfigFactoryMock;

  @Before
  public void setup() {
    pluginConfig = PluginConfig.Update.forTest(PLUGIN_NAME, new Config());
    pluginConfig.setString("gcloudProject", "project");
    pluginConfig.setString("subscriptionId", "sub");
    pluginConfig.setString("privateKeyLocation", "/test/private.key");
    pluginConfig.setString("userSubProxyEndpoint", "proxy.example.com");

    when(pluginConfigFactoryMock.getFromGerritConfig(PLUGIN_NAME))
        .thenReturn(pluginConfig.asPluginConfig());

    PubSubConfiguration configuration =
        new PubSubConfiguration(pluginConfigFactoryMock, PLUGIN_NAME, null);
    endpointBuilder = new PushSubscriptionEndpointUrlBuilder(configuration);
  }

  @Test
  public void validEndpoint() {
    assertThat(endpointBuilder.build("example.com", "test"))
        .isEqualTo("https://example.com?token=test");
    assertThat(endpointBuilder.build("https://example.com", "test"))
        .isEqualTo("https://example.com?token=test");
    assertThat(endpointBuilder.build("https://example.com/events", "test"))
        .isEqualTo("https://example.com/events?token=test");
    assertThat(endpointBuilder.build("https://example.com", null)).isEqualTo("https://example.com");
    assertThat(endpointBuilder.build("https://example.com", " ")).isEqualTo("https://example.com");
  }

  @Test
  public void invalidEndpoint() {
    assertThrows(IllegalArgumentException.class, () -> endpointBuilder.build(" ", "test"));
    assertThrows(
        IllegalArgumentException.class, () -> endpointBuilder.build("http://example.com", "test"));
    assertThrows(
        IllegalArgumentException.class, () -> endpointBuilder.build("ssh://example.com", "test"));
  }

  @Test
  public void validEndpointProxied() {
    assertThat(endpointBuilder.buildProxied("example.com", "test"))
        .isEqualTo("https://proxy.example.com?host=example.com&token=test");
    assertThat(endpointBuilder.buildProxied("https://example.com", "test"))
        .isEqualTo("https://proxy.example.com?host=example.com&token=test");
    assertThat(endpointBuilder.buildProxied("https://example.com/events", "test"))
        .isEqualTo("https://proxy.example.com?host=example.com&path=/events&token=test");
    assertThat(endpointBuilder.buildProxied("https://example.com", null))
        .isEqualTo("https://proxy.example.com?host=example.com");
    assertThat(endpointBuilder.buildProxied("https://example.com", " "))
        .isEqualTo("https://proxy.example.com?host=example.com");
  }

  @Test
  public void invalidEndpointProxied() {
    assertThrows(IllegalArgumentException.class, () -> endpointBuilder.buildProxied(" ", "test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> endpointBuilder.buildProxied("http://example.com", "test"));
    assertThrows(
        IllegalArgumentException.class,
        () -> endpointBuilder.buildProxied("ssh://example.com", "test"));
  }
}
