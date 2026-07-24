package com.ledgerly.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 32-byte secret (256 bits) for HS256, base64-encoded
        String secret = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        jwtService = new JwtService(secret, 30);
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldValidateAndExtractUsername() {
        String token = jwtService.generateToken("admin", "ADMIN");
        Jwt jwt = jwtService.validateToken(token);
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThatThrownBy(() -> jwtService.validateToken("invalid.token.here")).isInstanceOf(Exception.class);
    }

    @Test
    void shouldRejectExpiredToken() {
        String secret = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        JwtService shortLived = new JwtService(secret, 0);
        String token = shortLived.generateToken("admin", "ADMIN");
        // exp = iat + 0 minutes = already in the past
        assertThatThrownBy(() -> shortLived.validateToken(token)).isInstanceOf(Exception.class);
    }

    @Test
    void shouldReturnExpirationSeconds() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(1800);
    }
}