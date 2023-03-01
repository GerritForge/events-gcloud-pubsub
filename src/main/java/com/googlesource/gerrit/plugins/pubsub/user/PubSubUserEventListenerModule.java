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
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PubSubUserEventListenerModule extends AbstractModule {
  @Override
  public void configure() {
    bind(PubSubUserTopicNameFactory.class);
    bind(PubSubUserScopedEventListener.Factory.class);
    bind(new TypeLiteral<ConcurrentMap<Account.Id, PubSubRegistrationHandle>>() {})
        .annotatedWith(PubSubUserEventListenerHandlers.class)
        .toInstance(new ConcurrentHashMap<Account.Id, PubSubRegistrationHandle>());
  }
}
