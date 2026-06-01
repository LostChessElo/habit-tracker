package com.tracker.habit.auth;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String secret;

    public String generateToken(Long userId, Long expirationPeriod) {
        if (expirationPeriod == null) throw new NullPointerException("Expiration period can not be null");
        if (expirationPeriod < 0L) throw new IllegalArgumentException("Expiration period can not be negative");
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * expirationPeriod)) // 1 day
                .signWith(getSigningKey())
                .compact();
    }

    //default to a day
    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId)) // payload
                .issuedAt(new Date()) // time when issued
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // time when expires -> default to 1 day
                .signWith(getSigningKey()) // sign the token
                .compact();
    }

    public Long extractUserId(String token) {
        String payload = Jwts.parser()
                .verifyWith(getSigningKey()) // verify token signature
                .build()
                .parseSignedClaims(token) // throws an exception if token was tampered with
                .getPayload()
                .getSubject();

        return Long.parseLong(payload);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
