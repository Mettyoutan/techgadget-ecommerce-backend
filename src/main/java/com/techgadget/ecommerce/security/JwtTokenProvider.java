package com.techgadget.ecommerce.security;

import com.techgadget.ecommerce.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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

    private final long jwtExpirationInMs;
    private final SecretKey key;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration}") long jwtExpirationInMs
    ) {
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate JWT token for user
     * Has additional userId, email, and role
     */
    public String generateToken(Long userId, String email) {
        log.debug("Generating JWT token for userId={} and email={}.", userId, email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
        log.debug("Successfully generated token for userId={} and email={}.", userId, email);
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
