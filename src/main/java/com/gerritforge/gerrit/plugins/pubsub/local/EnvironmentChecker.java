// Copyright (C) 2025 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.pubsub.local;

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
