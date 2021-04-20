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

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;

@Singleton
public class PublisherProvider {

  protected CredentialsProvider credentials;
  protected PubSubConfiguration config;

  @Inject
  public PublisherProvider(CredentialsProvider credentials, PubSubConfiguration config) {
    this.credentials = credentials;
    this.config = config;
  }

  public Publisher get(String topic) throws IOException {
    return Publisher.newBuilder(TopicName.of(config.getGCloudProject(), topic))
        .setCredentialsProvider(credentials)
        .build();
  }
}
