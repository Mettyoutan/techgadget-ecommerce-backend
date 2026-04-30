package com.techgadget.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techgadget.ecommerce.dto.response.ErrorResponse;
import com.techgadget.ecommerce.enums.RateLimitTier;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH = "/api/auth";

    private final RateLimitService rateLimitService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /**
     * Check if request is allowed by rate limit filter
     * -
     * > .isAllowed() automatically add request to rate limit if allowed
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain
    ) throws ServletException, IOException {

        // Path always starts with /api
        String path = request.getRequestURI();
        RateLimitTier tier = resolveTier(path, request.getMethod());
        String key = resolveKey(request, tier);

        log.debug("Rate limit check: path={}, tier={}, key={}", path, tier, key);

        if (!rateLimitService.isAllowed(key, tier)) {
            long retryAfter = rateLimitService.getRetryAfterSeconds(key, tier);
            log.warn("Rate limit exceeded: key={}, tier={}, retryAfter={}", key, tier, retryAfter);
            writeRateLimitExceededResponse(response, retryAfter);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve rate limit tier based on path & method
     */
    private RateLimitTier resolveTier(String path, String method) {
        // Auth tier
        if (path.startsWith(AUTH_PATH) && method.equals("POST")) {
            return RateLimitTier.AUTH;
        }

        // Write tier
        if (!method.equals("GET")) {
            return RateLimitTier.WRITE;
        }

        // Default - read tier
        return RateLimitTier.READ;
    }

    /**
     * Resolve key for rate limiting
     * > Auth tier -> use IP
     * > Other tier -> use userId if exists, else use IP
     */
    private String resolveKey(HttpServletRequest request, RateLimitTier tier) {
        String clientIp = getClientIp(request);

        // Auth tier using ip:auth
        if (tier == RateLimitTier.AUTH) {
            return "ip:auth:" + clientIp;
        }

        // Authenticated request (WRITE, some READ) using user id
        String userId = extractUserIdFromJwt(request);
        if (userId != null) {
            return "user:" + userId;
        }

        // Public request (most READ) using ip:pub
        return "ip:pub:" + clientIp;
    }

    /**
     * Extract userId from Jwt token (optional)
     */
    private String extractUserIdFromJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        try {
            String token = header.substring(7);
            Claims claims = jwtTokenProvider.validateToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get client IP using X-Forwarded-For
     * in case request is behind proxy / load balancer
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Write 429 (Too Many Requests) response
     * -
     * Using ErrorResponse format
     */
    private void writeRateLimitExceededResponse(
            HttpServletResponse response, long retryAfter) throws IOException {

        response.setContentType("application/json");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        // Set Retry-After header
        response.setHeader("Retry-After", String.valueOf(retryAfter));

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.setCode(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        errorResponse.setMessage("Too many requests. Please try again in " + retryAfter + " seconds.");

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}
