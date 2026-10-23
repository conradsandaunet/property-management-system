package org.conrad.gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Limits POST /api/auth/login attempts per client address (in memory, per gateway instance).
class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int CLEANUP_THRESHOLD = 1000;

    private final int attempts;
    private final Duration period;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    LoginRateLimitFilter(int attempts, Duration period) {
        this.attempts = attempts;
        this.period = period;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (buckets.size() > CLEANUP_THRESHOLD) {
            // A full bucket means the client has been idle for a whole period. safe to forget.
            buckets.values().removeIf(b -> b.getAvailableTokens() >= attempts);
        }

        Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1);
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(attempts).refillGreedy(attempts, period).build())
                .build();
    }
}
