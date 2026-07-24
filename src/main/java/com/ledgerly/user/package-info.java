/**
 * User module: owns the user aggregate (authentication accounts, not customers).
 * Provides a public UserService for the auth module to load users by username.
 */
@org.springframework.modulith.ApplicationModule(
    id = "user",
    displayName = "User Management"
)
package com.ledgerly.user;
