package com.tracker.habit.auth;

import com.tracker.habit.exception.ApiException;
import com.tracker.habit.user.User;
import com.tracker.habit.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service handling user authentication: registration and login.
 *
 * <p>On success both operations return a signed JWT that the client
 * must include as a {@code Bearer} token on all subsequent requests.</p>
 */

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

    /**
     * Authenticates a user by email and password.
     *
     * <p>A generic "Invalid credentials" error is returned for both unknown
     * email addresses and wrong passwords to prevent user enumeration.</p>
     *
     * @param email    the user's email address
     * @param password the plaintext password to verify
     * @return a signed JWT for the authenticated user
     * @throws ApiException with {@code 401 UNAUTHORIZED} if credentials are invalid
     */
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

    /**
     * Registers a new user account.
     *
     * <p>The password is hashed with BCrypt before persistence.
     * The newly created user is immediately signed in, a JWT is returned
     * so the client does not need a separate login call.</p>
     *
     * @param email    the desired email address; must be unique
     * @param password the plaintext password, which will be hashed
     * @return a signed JWT for the newly created user
     * @throws ApiException with {@code 409 CONFLICT} if the email is already registered
     */
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
