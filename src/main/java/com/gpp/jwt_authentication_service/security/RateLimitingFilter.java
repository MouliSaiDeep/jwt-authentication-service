package com.gpp.jwt_authentication_service.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().equals("/auth/login") && request.getMethod().equals("POST")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip);
            
            io.github.bucket4j.ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(request, response);
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.setStatus(429);
                response.setHeader("X-RateLimit-Limit", "5");
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("Retry-After", String.valueOf(waitForRefill));
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded for login.\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
