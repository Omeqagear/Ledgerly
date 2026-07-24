/**
 * Auth module: JWT token issuance and authentication. Depends on the
 * {@code user} module to load user credentials. Exposes a login endpoint
 * and a JwtService for token generation/validation.
 */
@org.springframework.modulith.ApplicationModule(
    id = "auth",
    displayName = "Authentication",
    allowedDependencies = {"user"}
)
package com.ledgerly.auth;
