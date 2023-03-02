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

import org.junit.Test;

public class PushSubscriptionEndpointBuilderTest {
  private final PushSubscriptionEndpointUrlBuilder endpointBuilder =
      new PushSubscriptionEndpointUrlBuilder();

  @Test
  public void validEndpoint() {
    assertThat(endpointBuilder.build("example.com", "test"))
        .isEqualTo("https://example.com?token=test");
    assertThat(endpointBuilder.build("https://example.com", "test"))
        .isEqualTo("https://example.com?token=test");
    assertThat(endpointBuilder.build("https://example.com", null)).isEqualTo("https://example.com");
    assertThat(endpointBuilder.build("https://example.com", " ")).isEqualTo("https://example.com");
  }

  @Test
  public void invalidEndpoint() {
    assertThrows(
        IllegalArgumentException.class, () -> endpointBuilder.build("http://example.com", "test"));
    assertThrows(
        IllegalArgumentException.class, () -> endpointBuilder.build("ssh://example.com", "test"));
  }
}
