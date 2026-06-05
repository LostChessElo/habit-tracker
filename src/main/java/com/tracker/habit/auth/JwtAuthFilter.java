package com.tracker.habit.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that validates the JWT on every incoming request.
 *
 * <p>The filter reads the {@code Authorization: Bearer <token>} header,
 * verifies the token signature and expiry via {@link JwtUtil}, then loads
 * the extracted user ID into the {@link SecurityContextHolder} as the
 * authentication principal. Downstream controllers access it via
 * {@code (Long) authentication.getPrincipal()}.</p>
 *
 * <p>Requests without a {@code Bearer} header, or with an invalid token,
 * are passed through without authentication and Spring Security's access
 * rules will reject them at the authorization layer if the route requires
 * authentication.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil util;

    public JwtAuthFilter(JwtUtil util) {
        this.util = util;
    }

    /**
     * Extracts and validates the JWT from the {@code Authorization} header,
     * then populates the security context if the token is valid.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain to invoke
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // remove substring "bearer "
        String token = header.substring(7).trim();

        try {
            Long uid = util.extractUserId(token);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(uid, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            // Spring Security will handle the 401 if the route requires it.
            filterChain.doFilter(request, response);
        }

    }
}
