package com.googlesource.gerrit.plugins.pubsub.user;

import com.google.gerrit.extensions.registration.RegistrationHandle;

public class PubSubRegistrationHandle implements RegistrationHandle {
  private final RegistrationHandle handle;
  private final PubSubUserScopedEventListener eventListener;

  public PubSubRegistrationHandle(
      RegistrationHandle handle, PubSubUserScopedEventListener eventListener) {
    this.handle = handle;
    this.eventListener = eventListener;
  }

  @Override
  public void remove() {
    try {
      eventListener.disconnect();
    } finally {
      handle.remove();
    }
  }
}
