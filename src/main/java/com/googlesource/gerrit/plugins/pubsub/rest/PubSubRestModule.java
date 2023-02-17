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

package com.googlesource.gerrit.plugins.pubsub.rest;

import static com.google.gerrit.server.account.AccountResource.ACCOUNT_KIND;

import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.pubsub.PubSubConfiguration;

public class PubSubRestModule extends RestApiModule {
  private final PubSubConfiguration configuration;

  @Inject
  public PubSubRestModule(PubSubConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    if (configuration.isEnableUserStreamEvents()) {
      bind(PubSubUserTopicNameFactory.class);

      put(ACCOUNT_KIND, "pubsub.topic").to(PutTopic.class);
      delete(ACCOUNT_KIND, "pubsub.topic").to(DeleteTopic.class);
    }
  }
}
