package com.tracker.habit.auth;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

/**
 * Utility component for creating and parsing JWT tokens.
 *
 * <p>Tokens are HMAC-SHA signed using the secret configured via
 * {@code app.jwt.secret} (Base64-encoded). The subject claim stores
 * the numeric user ID so the filter chain can reconstruct the principal
 * without a database lookup.</p>
 */
@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String secret;

    public String generateToken(Long userId, Long expirationSeconds) {
        Long seconds = Objects.requireNonNull(expirationSeconds);
        if (seconds < 0L) throw new IllegalArgumentException("Expiration period can not be negative");

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * seconds))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a JWT with a expiry of 24 hours.
     *
     * @param userId the ID of the user to embed as the token subject
     * @return a compact, signed JWT string valid for one day
     */
    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId)) // payload
                .issuedAt(new Date()) // time when issued
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // time when expires -> default to 1 day
                .signWith(getSigningKey()) // sign the token
                .compact();
    }

    /**
     * Validates a JWT and extracts the user ID from its subject claim.
     *
     * @param token the compact JWT string to parse
     * @return the user ID stored in the token subject
     * @throws io.jsonwebtoken.JwtException if the token is expired, malformed, or has an invalid signature
     */
    public Long extractUserId(String token) {
        String payload = Jwts.parser()
                .verifyWith(getSigningKey()) // verify token signature
                .build()
                .parseSignedClaims(token) // throws an exception if token was tampered with
                .getPayload()
                .getSubject();

        return Long.parseLong(payload);
    }

    /**
     * Decodes the Base64 secret and constructs the HMAC signing key.
     *
     * @return the {@link SecretKey} used for signing and verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
