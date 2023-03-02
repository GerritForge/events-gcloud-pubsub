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

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.googlesource.gerrit.plugins.pubsub.local.EnvironmentChecker;
import com.googlesource.gerrit.plugins.pubsub.local.LocalPushSubscriptionEndpointUrlBuilder;
import com.googlesource.gerrit.plugins.pubsub.local.LocalUserSubscriptionProvider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PubSubUserEventListenerModule extends AbstractModule {
  private final EnvironmentChecker environmentChecker;

  public PubSubUserEventListenerModule(EnvironmentChecker environmentChecker) {
    this.environmentChecker = environmentChecker;
  }

  @Override
  public void configure() {
    DynamicSet.bind(binder(), LifecycleListener.class).to(PubSubUserEventListenerManager.class);

    bind(PubSubUserTopicNameFactory.class);
    bind(PubSubUserScopedEventListener.Factory.class);
    bind(new TypeLiteral<ConcurrentMap<Account.Id, PubSubRegistrationHandle>>() {})
        .annotatedWith(PubSubUserEventListenerHandlers.class)
        .toInstance(new ConcurrentHashMap<Account.Id, PubSubRegistrationHandle>());

    if (environmentChecker.isLocalEnvironment()) {
      bind(PushSubscriptionEndpointUrlBuilder.class)
          .to(LocalPushSubscriptionEndpointUrlBuilder.class);
      bind(PubSubUserSubscriptionProvider.class).to(LocalUserSubscriptionProvider.class);
    } else {
      bind(PushSubscriptionEndpointUrlBuilder.class);
      bind(PubSubUserSubscriptionProvider.class);
    }
  }
}
