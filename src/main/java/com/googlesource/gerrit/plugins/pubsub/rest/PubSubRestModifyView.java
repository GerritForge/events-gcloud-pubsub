package com.googlesource.gerrit.plugins.pubsub.rest;

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Provider;
import java.io.IOException;

public abstract class PubSubRestModifyView implements RestModifyView<AccountResource, Input> {

  private final Provider<CurrentUser> userProvider;
  private final PermissionBackend permissionBackend;

  public PubSubRestModifyView(
      Provider<CurrentUser> userProvider, PermissionBackend permissionBackend) {
    this.userProvider = userProvider;
    this.permissionBackend = permissionBackend;
  }

  abstract Response<?> applyImpl(AccountResource rsrc, Input input) throws IOException;

  @Override
  public Response<?> apply(AccountResource rsrc, Input input)
      throws IOException, AuthException, MethodNotAllowedException {
    checkPermission(rsrc.getUser());
    return applyImpl(rsrc, input);
  }

  protected void checkPermission(IdentifiedUser user)
      throws AuthException, MethodNotAllowedException {
    CurrentUser requestingUser = userProvider.get();
    if (requestingUser == null || !requestingUser.isIdentifiedUser()) {
      throw new AuthException("authentication required");
    }

    if (!requestingUser.getAccountId().equals(user.getAccountId())
        && !permissionBackend.user(requestingUser).testOrFalse(ADMINISTRATE_SERVER)) {
      throw new MethodNotAllowedException("Forbidden");
    }
  }
}
