package com.techgadget.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final long accessExpirationInMs;
    private final long refreshExpirationInMs;
    private final SecretKey key;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-expiration}") long accessExpirationInMs,
            @Value("${app.jwt.refresh-expiration}") long refreshExpirationInMs
    ) {
        this.accessExpirationInMs = accessExpirationInMs;
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.refreshExpirationInMs = refreshExpirationInMs;
    }

    /**
     * Generate JWT access token for user
     * - Exp : 15 minutes
     * Has additional userId, email, and role
     */
    public String generateAccessToken(Long userId, String email) {
        log.debug("Generating JWT access token for user {} and email {}.", userId, email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpirationInMs);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
        log.debug("Successfully generated access token for user {} and email {}.",
                userId, email);
        return token;
    };

    /**
     * Generate JWT refresh token for user
     * - Exp : 7 days
     * Has additional userId, email, and role
     */
    public String generateRefreshToken(Long userId, String email) {
        log.debug("Generating JWT refresh token for user {} and email {}.", userId, email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
        log.debug("Successfully generated refresh token for user {} and email {}.",
                userId, email);
        return token;
    };

    public Claims validateToken(String token) {
        log.debug("Validating JWT token for token={}", token);
        try {
            Claims claim = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.debug("Successfully validated token for token={}", token);
            return claim;
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token for token={}", token);
            throw e;
        } catch (Exception e) {
            log.warn("Something went wrong while validating JWT token for token={}.", token);
            throw e;
        }
    }
}
