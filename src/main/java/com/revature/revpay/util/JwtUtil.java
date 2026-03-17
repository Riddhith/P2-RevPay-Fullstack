package com.revature.revpay.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility component for generating and validating JWT tokens.
 * Tokens are signed with HS256 and contain the user's email (subject)
 * and role as a claim.
 */
@Component
public class JwtUtil {

    private static final Logger log = LogManager.getLogger(JwtUtil.class);

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Generate a signed JWT token for the given email and role.
     *
     * @param email the user's email (used as JWT subject)
     * @param role  the user's role, e.g. "PERSONAL" or "BUSINESS"
     * @return compact JWT string
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract the email (subject) from a JWT token.
     *
     * @param token compact JWT string
     * @return email string, or null if token is invalid
     */
    public String extractEmail(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract the role claim from a JWT token.
     *
     * @param token compact JWT string
     * @return role string, or null if token is invalid
     */
    public String extractRole(String token) {
        try {
            return parseClaims(token).get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate a JWT token — checks signature and expiry.
     *
     * @param token compact JWT string
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
