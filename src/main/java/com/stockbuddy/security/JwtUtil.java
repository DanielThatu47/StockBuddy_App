// src/main/java/com/stockbuddy/security/JwtUtil.java
package com.stockbuddy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // ms — 7 days = 604800000

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT for a user. When {@code sessionId} is set, claim {@code sid} binds the token
     * to {@link com.stockbuddy.model.UserSession} so revoking that session invalidates the token.
     */
    public String generateToken(String userId) {
        return generateToken(userId, null);
    }

    public String generateToken(String userId, String sessionId) {
        var builder = Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey());
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId.trim());
        }
        return builder.compact();
    }

    /**
     * Extract userId claim from token.
     */
    public String extractUserId(String token) {
        return getClaims(token).get("userId", String.class);
    }

    /** Session id claim; null if absent (legacy tokens). */
    public String extractSessionId(String token) {
        try {
            String sid = getClaims(token).get("sid", String.class);
            return (sid != null && !sid.isBlank()) ? sid.trim() : null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Validate a token; throws JwtException if invalid/expired.
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
          
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
