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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.registration.RegistrationHandle;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.UserScopedEventListener;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.pubsub.PubSubPublisher;
import java.util.concurrent.ConcurrentMap;

public class PubSubUserScopedEventListener implements UserScopedEventListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final IdentifiedUser user;
  private final PubSubPublisher publisher;

  public static class Factory {
    private final PubSubUserTopicNameFactory topicNameFactory;
    private final PubSubPublisher.Factory publisherFactory;
    private final ConcurrentMap<Account.Id, PubSubRegistrationHandle>
        pubSubUserStreamEventListenerHandlers;
    private final DynamicSet<UserScopedEventListener> eventListeners;
    private final String pluginName;

    @Inject
    private Factory(
        PubSubUserTopicNameFactory topicNameFactory,
        PubSubPublisher.Factory publisherFactory,
        @PubSubUserEventListenerHandlers
            ConcurrentMap<Account.Id, PubSubRegistrationHandle>
                pubSubUserStreamEventListenerHandlers,
        DynamicSet<UserScopedEventListener> eventListeners,
        @PluginName String pluginName) {
      this.topicNameFactory = topicNameFactory;
      this.publisherFactory = publisherFactory;
      this.pubSubUserStreamEventListenerHandlers = pubSubUserStreamEventListenerHandlers;
      this.eventListeners = eventListeners;
      this.pluginName = pluginName;
    }

    public PubSubUserScopedEventListener create(IdentifiedUser user) {
      logger.atInfo().log("Created PubSub EventListener for account %s", user.getAccountId());
      PubSubUserScopedEventListener eventListener =
          new PubSubUserScopedEventListener(
              publisherFactory.create(
                  topicNameFactory.createForAccount(user.getAccountId()).getTopic()),
              user);
      RegistrationHandle handle = eventListeners.add(pluginName, eventListener);
      pubSubUserStreamEventListenerHandlers.put(
          user.getAccountId(), new PubSubRegistrationHandle(handle, eventListener));
      return eventListener;
    }
  }

  PubSubUserScopedEventListener(PubSubPublisher publisher, IdentifiedUser user) {
    this.user = user;
    this.publisher = publisher;
  }

  @Override
  public void onEvent(Event event) {
    publisher.publish(event);
  }

  @Override
  public CurrentUser getUser() {
    return user;
  }

  public void disconnect() {
    try {
      publisher.close();
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("Disconnect failed");
    }
  }
}
