// src/main/java/com/stockbuddy/security/JwtAuthFilter.java
package com.stockbuddy.security;

import com.stockbuddy.model.UserSession;
import com.stockbuddy.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Applied only inside Spring Security's filter chain (not as a servlet-container
 * filter) so public routes like forgot-password follow {@code authorizeHttpRequests} only.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionRepository sessionRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, SessionRepository sessionRepository) {
        this.jwtUtil = jwtUtil;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains("/api/forgot-password");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.extractUserId(token);
                    String sid = jwtUtil.extractSessionId(token);
                    if (sid != null && !sid.isBlank()) {
                        Optional<UserSession> sess = sessionRepository.findById(sid);
                        if (sess.isEmpty()
                                || !userId.equals(sess.get().getUserId())
                                || !sess.get().isActive()) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Session ended or invalid. Please sign in again.\"}");
                            return;
                        }
                    }

                    // Store userId as the principal so controllers can retrieve it
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Also set as request attribute for convenience
                    request.setAttribute("userId", userId);
                } else if (!token.isEmpty()) {
                    logger.warn("JWT rejected for {} (invalid signature, expired, or malformed)");
                }
            } catch (Exception e) {
                logger.warn("JWT validation failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
