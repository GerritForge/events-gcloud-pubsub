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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;

@Singleton
public class PushSubscriptionEndpointUrlBuilder {
  private String userSubProxyEndpoint;

  @Inject
  public PushSubscriptionEndpointUrlBuilder(PubSubConfiguration pubSubProperties) {
    this.userSubProxyEndpoint = pubSubProperties.getUserSubProxyEndpoint();
  }

  public String getProtocol() {
    return "https";
  }

  public String build(String pushEndpoint, String verificationToken) {
    if (pushEndpoint == null || pushEndpoint.isBlank()) {
      throw new IllegalArgumentException("Pushendpoint is required.");
    }
    StringBuilder uri = new StringBuilder();

    int protocolEnd = pushEndpoint.indexOf("://");
    if (protocolEnd > -1) {
      if (!pushEndpoint.substring(0, protocolEnd).equals(getProtocol())) {
        throw new IllegalArgumentException("Protocol has to be " + getProtocol());
      }
    } else {
      uri.append(getProtocol());
      uri.append("://");
    }
    uri.append(pushEndpoint);
    if (verificationToken != null && !verificationToken.isBlank()) {
      uri.append("?token=");
      uri.append(verificationToken);
    }
    return uri.toString();
  }

  public String buildProxied(String pushEndpoint, String verificationToken) {
    if (userSubProxyEndpoint == null || userSubProxyEndpoint.isBlank()) {
      throw new IllegalArgumentException("Can't push to internal network. Proxy URL not set.");
    }
    StringBuilder uri = new StringBuilder();
    uri.append(userSubProxyEndpoint);
    uri.append("?host=");

    if (pushEndpoint == null || pushEndpoint.isBlank()) {
      throw new IllegalArgumentException("Pushendpoint is required.");
    }

    int protocolEnd = pushEndpoint.indexOf("://");
    if (protocolEnd > -1) {
      if (!pushEndpoint.substring(0, protocolEnd).equals(getProtocol())) {
        throw new IllegalArgumentException("Protocol has to be " + getProtocol());
      }
      pushEndpoint = pushEndpoint.substring(protocolEnd + 3);
    }

    String[] pushEndpointParts = pushEndpoint.split("/", 2);
    uri.append(pushEndpointParts[0]);

    if (pushEndpointParts.length == 2) {
      uri.append("&path=/");
      uri.append(pushEndpointParts[1]);
    }

    if (verificationToken != null && !verificationToken.isBlank()) {
      uri.append("&token=");
      uri.append(verificationToken);
    }
    return uri.toString();
  }
}
