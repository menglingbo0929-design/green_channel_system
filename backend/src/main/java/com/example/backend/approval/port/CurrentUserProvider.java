package com.example.backend.approval.port;

/**
 * Member-one integration point for the trusted login context.
 * Controllers must never accept role, user ID, or college ID from the client.
 */
public interface CurrentUserProvider {
    LoginUser getRequiredUser();
}
