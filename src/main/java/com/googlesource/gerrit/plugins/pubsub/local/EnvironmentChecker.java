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

package com.googlesource.gerrit.plugins.pubsub.local;

import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public class EnvironmentChecker {
  public static final String PUBSUB_EMULATOR_HOST = "PUBSUB_EMULATOR_HOST";

  private Optional<String> hostPort;

  public EnvironmentChecker() {
    this.hostPort =
        Optional.ofNullable(System.getenv(PUBSUB_EMULATOR_HOST))
            .or(() -> Optional.ofNullable(System.getProperty(PUBSUB_EMULATOR_HOST)));
  }

  public Optional<String> getLocalHostAndPort() {
    return hostPort;
  }

  public Boolean isLocalEnvironment() {
    return getLocalHostAndPort().isPresent();
  }
}
