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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PubSubEventListener implements EventListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PubSubPublisher publisher;

  @Inject
  public PubSubEventListener(
      PubSubPublisher.Factory publisherFactory, PubSubConfiguration configuration) {
    this.publisher = publisherFactory.create(configuration.getStreamEventsTopic());
  }

  @Override
  public void onEvent(Event event) {
    publisher.publish(event);
  }

  public void disconnect() {
    try {
      publisher.close();
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("Disconnect failed");
    }
  }
}
