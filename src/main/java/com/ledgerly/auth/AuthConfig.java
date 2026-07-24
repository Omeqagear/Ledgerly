package com.ledgerly.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class AuthConfig {

    @Bean
    SecretKey jwtSecretKey(@Value("${ledgerly.jwt.secret}") String base64Secret) {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(base64Secret);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtService jwtService(SecretKey jwtSecretKey, @Value("${ledgerly.jwt.expiration-minutes:30}") long expirationMinutes) {
        String base64Secret = java.util.Base64.getEncoder().encodeToString(jwtSecretKey.getEncoded());
        return new JwtService(base64Secret, expirationMinutes);
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
