package com.tracker.habit.auth;

import com.tracker.habit.exception.ApiException;
import com.tracker.habit.user.User;
import com.tracker.habit.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private final String email = "test@example.com";
    private final String password = "password123";
    private final String hash = "$2a$10$hashedpassword";
    private final String token = "jwt.token.here";
    private final User existingUser = new User(
            1L, "test@example.com",
            "$2a$10$hashedpassword", LocalDateTime.now()
    );


    // test login
    @Test
    void login_emailNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(email, password));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(password, existingUser.passwordHash())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(email, password));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void login_validCredentials_returnsToken() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(password, existingUser.passwordHash())).thenReturn(true);
        when(jwtUtil.generateToken(existingUser.id())).thenReturn(token);

        String result = authService.login(email, password);

        assertEquals(token, result);
    }

    // Test registration
    @Test
    void register_emailAlreadyExists_throwsConflict() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.register(email, password));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void register_validRequest_returnsToken() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(hash);
        when(userRepository.save(email, hash)).thenReturn(1L);
        when(jwtUtil.generateToken(1L)).thenReturn(token);

        String result = authService.register(email, password);

        assertEquals(token, result);
    }

    @Test
    void register_validRequest_passwordIsHashed() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(hash);
        when(userRepository.save(email, hash)).thenReturn(1L);
        when(jwtUtil.generateToken(1L)).thenReturn(token);

        authService.register(email, password);

        // verify encode was called with the raw password
        verify(passwordEncoder).encode(password);
        // verify save was never called with the raw password
        verify(userRepository, never()).save(email, password);
    }
}