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

import com.gerritforge.gerrit.eventbroker.EventGsonProvider;
import com.google.api.gax.core.CredentialsProvider;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.EventListener;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.pubsub.local.EnvironmentChecker;
import com.googlesource.gerrit.plugins.pubsub.local.LocalCredentialsProvider;
import com.googlesource.gerrit.plugins.pubsub.local.LocalPublisherProvider;
import com.googlesource.gerrit.plugins.pubsub.local.LocalSubscriberProvider;

class Module extends FactoryModule {

  private PubSubApiModule pubSubApiModule;
  private EnvironmentChecker environmentChecker;

  @Inject
  public Module(PubSubApiModule pubSubApiModule, EnvironmentChecker environmentChecker) {
    this.pubSubApiModule = pubSubApiModule;
    this.environmentChecker = environmentChecker;
  }

  @Override
  protected void configure() {
    bind(Gson.class).toProvider(EventGsonProvider.class).in(Singleton.class);
    DynamicSet.bind(binder(), LifecycleListener.class).to(Manager.class);
    DynamicSet.bind(binder(), EventListener.class).to(PubSubEventListener.class);
    factory(PubSubPublisher.Factory.class);
    factory(PubSubEventSubscriber.Factory.class);

    if (environmentChecker.isLocalEnvironment()) {
      bind(CredentialsProvider.class)
          .toProvider(LocalCredentialsProvider.class)
          .in(Scopes.SINGLETON);
      bind(SubscriberProvider.class).to(LocalSubscriberProvider.class);
      bind(PublisherProvider.class).to(LocalPublisherProvider.class);
    } else {
      bind(CredentialsProvider.class)
          .toProvider(ServiceAccountCredentialsProvider.class)
          .in(Scopes.SINGLETON);
      bind(SubscriberProvider.class);
      bind(PublisherProvider.class);
    }
    install(pubSubApiModule);
  }
}
