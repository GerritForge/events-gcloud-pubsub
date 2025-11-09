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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.inject.Provider;

public class LocalCredentialsProvider implements Provider<CredentialsProvider> {

  CredentialsProvider credentials;

  public LocalCredentialsProvider() {
    this.credentials =
        FixedCredentialsProvider.create(NoCredentialsProvider.create().getCredentials());
  }

  @Override
  public CredentialsProvider get() {
    return credentials;
  }
}
