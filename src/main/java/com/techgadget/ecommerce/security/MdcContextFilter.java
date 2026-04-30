package com.techgadget.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Injects business-level context into SLF4J MDC after authentication.
 *
 * Why this filter exists:
 *   Micrometer Tracing automatically injects traceId and spanId into MDC
 *   for every HTTP request — we don't need to do that ourselves.
 *   However, Micrometer knows nothing about <b>who</b> is making the request.
 *   This filter runs after JwtAuthenticationFilter has populated
 *   SecurityContextHolder, reads the <b>authenticated user's</b> ID from there,
 *   and adds it to MDC so that every log line in the request automatically
 *   carries the userId field — without passing it as a parameter everywhere.
 *
 *   Even though Authentication is not populated, <b>IP address</b> always being putted to MDC
 *
 * Filter order:
 *   ... → JwtAuthenticationFilter → MdcContextFilter → ...
 *
 * MDC cleanup:
 *   MDC is backed by ThreadLocal (or virtual-thread-local in Java 21+).
 *   Spring's thread pool recycles threads between requests. If we don't
 *   clear MDC in the finally block, stale userId from a previous request
 *   could leak into an unrelated subsequent request on the same thread.
 *   The try/finally pattern guarantees cleanup even if an exception occurs.
 */
@Component
@Slf4j
public class MdcContextFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_IP_ADDRESS = "ipAddress";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {

            // JwtAuthenticationFilter has already run at this point.
            // If the request had a valid token, SecurityContextHolder is populated.
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Put ip address
            String ip = getIpAddress(request);
            MDC.put(MDC_IP_ADDRESS, ip);
            log.debug("MDC ipAddress {} is populated", ip);

            // If authentication exists, put user id
            if (auth != null && auth.isAuthenticated() &&
                    auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                // Put userId into MDC. From this point forward, every log statement
                // in this thread — regardless of which class writes it — will
                // automatically include "userId": "42" in its output.
                MDC.put(MDC_USER_ID, String.valueOf(userDetails.getUserId()));

                log.debug("MDC userId {} is populated", userDetails.getUserId());
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clean up MDC fields we added.
            // Note: we only remove what we added — traceId and spanId are managed
            // by Micrometer and will be cleaned up by its own infrastructure.
            MDC.clear();
        }

    }

    // Helper method to get IP address
    private String getIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
