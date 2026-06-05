package com.tracker.habit.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private Long id;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        id = 123456789L;
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                Base64.getEncoder().encodeToString(
                        "a-test-secret-that-is-long-enough-for-hmac".getBytes()));
    }

    @Test
    void generatedToken_isNotBlank() {
        String token = jwtUtil.generateToken(id);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUserId_returnsCorrectId() {
        String token = jwtUtil.generateToken(id);
        Long uid = jwtUtil.extractUserId(token);
        assertEquals(id, uid);
    }

    @Test
    void tamperedToken_throwsException() {
        String token = jwtUtil.generateToken(id);

        String[] parts = token.split("\\.");
        String signature = parts[2];
        int midIndex = signature.length() / 2;
        char midChar = signature.charAt(midIndex);
        char tamperedChar = (midChar == 'A') ? 'B' : 'A';
        String tamperedSignature = signature.substring(0, midIndex) + tamperedChar + signature.substring(midIndex + 1);
        String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThrows(JwtException.class, () -> jwtUtil.extractUserId(tamperedToken));
    }

    @Test
    void generateToken_nullExpiration_throwsException() {
        assertThrows(NullPointerException.class, () -> jwtUtil.generateToken(id, null));
    }

    @Test
    void generateToken_negativeExpiration_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> jwtUtil.generateToken(id, -1L));
    }

    @Test
    void expiredToken_throwsException() throws InterruptedException {
        String token = jwtUtil.generateToken(id, 1L);
        Thread.sleep(1100);
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUserId(token));
    }
}