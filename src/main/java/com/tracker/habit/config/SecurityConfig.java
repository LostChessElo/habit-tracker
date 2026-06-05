package com.tracker.habit.config;

import com.tracker.habit.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the application.
 *
 * <p>Configures a stateless, JWT-based security model:</p>
 * <ul>
 *   <li>Sessions are never created, every request must carry its own JWT.</li>
 *   <li>CSRF protection is disabled, as it is not needed for stateless REST APIs.</li>
 *   <li>{@code /api/auth/**} is publicly accessible (register and login).</li>
 *   <li>All other routes require a valid JWT processed by {@link JwtAuthFilter}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter authFilter;

    public SecurityConfig(JwtAuthFilter auth) {
        this.authFilter = auth;
    }

    /**
     * Defines the security filter chain applied to every HTTP request.
     *
     * <p>{@link JwtAuthFilter} runs before Spring's default
     * {@link UsernamePasswordAuthenticationFilter}, so the JWT principal is
     * available to all downstream filters and controllers.</p>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration fails to build
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests( auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // register and login are public
                .anyRequest().authenticated());
        return http.build();
    }

    /**
     * Provides a BCrypt password encoder as a Spring-managed bean.
     *
     * <p>BCrypt automatically handles salting, making it resistant to
     * rainbow table attacks. The encoder is injected into {@code AuthService}
     * for hashing passwords on registration and verifying them on login.</p>
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
