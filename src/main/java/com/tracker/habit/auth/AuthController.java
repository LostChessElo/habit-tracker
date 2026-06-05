package com.tracker.habit.auth;

import com.tracker.habit.auth.dto.AuthResponse;
import com.tracker.habit.auth.dto.LoginRequest;
import com.tracker.habit.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication.
 *
 * <p>All endpoints are public ({@code /api/auth/**} is excluded from the
 * JWT filter) and return a {@link AuthResponse} containing a signed JWT
 * on success.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /**
     * Authenticates an existing user and returns a JWT.
     *
     * @param loginRequest the login payload containing {@code email} and {@code password}
     * @return {@code 200 OK} with a {@link AuthResponse} containing the JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        String jwt = service.login(loginRequest.email(), loginRequest.password());
        AuthResponse response = new AuthResponse(jwt);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user and returns a JWT.
     *
     * <p>The client can use the returned token immediately, no separate
     * login step is required after registration.</p>
     *
     * @param registerRequest the registration payload containing {@code email} and {@code password}
     * @return {@code 201 CREATED} with a {@link AuthResponse} containing the JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        String jwt = service.register(registerRequest.email(), registerRequest.password());
        AuthResponse response = new AuthResponse(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
