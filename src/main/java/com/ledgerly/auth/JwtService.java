package com.ledgerly.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expirationMinutes;

    public JwtService(String base64Secret, long expirationMinutes) {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Secret);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(secretKey);
        this.encoder = new NimbusJwtEncoder(jwkSource);
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        nimbusDecoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        this.decoder = nimbusDecoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        Instant iat = exp.isAfter(now) ? now : exp.minusNanos(1);
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(iat)
            .expiresAt(exp)
            .build();
        return encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public Jwt validateToken(String token) {
        return decoder.decode(token);
    }

    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}