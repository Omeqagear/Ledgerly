package com.ledgerly.user;

import java.util.UUID;

/**
 * Thrown when a user cannot be found by id or username.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }

    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
}
