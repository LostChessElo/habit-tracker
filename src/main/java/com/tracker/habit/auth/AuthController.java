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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        String jwt = service.login(loginRequest.email(), loginRequest.password());
        AuthResponse response = new AuthResponse(jwt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        String jwt = service.register(registerRequest.email(), registerRequest.password());
        AuthResponse response = new AuthResponse(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
