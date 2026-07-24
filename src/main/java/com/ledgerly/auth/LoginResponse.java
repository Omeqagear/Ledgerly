package com.ledgerly.auth;

public record LoginResponse(String token, long expiresIn, String username, String role) {}
