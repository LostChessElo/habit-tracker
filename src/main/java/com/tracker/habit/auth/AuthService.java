package com.tracker.habit.auth;

import com.tracker.habit.exception.ApiException;
import com.tracker.habit.user.User;
import com.tracker.habit.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtUtil util) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = util;
        this.userRepository = userRepository;
    }

    public String login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);

        // user doesnt exist
        if (user.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
        }

        String originalHash = user.get().passwordHash();
        boolean pwdMatches = passwordEncoder.matches(password, originalHash);

        if (!pwdMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
        }

        return jwtUtil.generateToken(user.get().id());

    }

    public String register(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);

        // user exists
        if (user.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists.");
        }

        Long uid = userRepository.save(email, passwordEncoder.encode(password));
        return jwtUtil.generateToken(uid);
    }
}
