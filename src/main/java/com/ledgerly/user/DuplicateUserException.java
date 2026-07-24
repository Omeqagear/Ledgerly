package com.ledgerly.user;

/**
 * Thrown when attempting to create a user with a username that already exists.
 */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String username) {
        super("Username already taken: " + username);
    }
}