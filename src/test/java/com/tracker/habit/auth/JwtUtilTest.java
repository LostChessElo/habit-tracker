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

        char lastChar = token.charAt(token.length() - 1);
        char tamperedChar = (lastChar == 'A') ? 'B' : 'A';
        String tamperedToken = token.substring(0, token.length() - 1) + tamperedChar;
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