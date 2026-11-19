package org.conrad.billingservice.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.conrad.billingservice.dto.ApartmentDto;
import org.conrad.billingservice.dto.ResidentDto;
import org.conrad.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Calls resident-service. The calls are protected against a service that is slow or gone:
 * <ul>
 *   <li>a connect and a read timeout, so a hanging service doesn't block our threads;</li>
 *   <li>a few retries with a short pause for failures that may pass (timeouts, connection errors, 5xx).
 *       All the calls are reads, so repeating them is safe;</li>
 *   <li>a circuit breaker that fails fast while resident-service keeps failing, then lets a trial call
 *       through to see if it is back.</li>
 * </ul>
 * Answers such as 401 or 404 are real answers: they are neither retried nor counted as failures.
 */
@Component
public class ResidentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResidentServiceClient.class);
    private static final String SERVICE_SUBJECT = "billing-service";

    private final RestClient restClient;
    private final JwtService jwtService;
    private final CircuitBreaker circuitBreaker;
    private final int retries;
    private final Duration retryPause;

    @Autowired
    public ResidentServiceClient(
            RestClient.Builder builder,
            JwtService jwtService,
            @Value("${resident-service.base-url}") String baseUrl,
            @Value("${resident-service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${resident-service.read-timeout:5s}") Duration readTimeout,
            @Value("${resident-service.retries:2}") int retries,
            @Value("${resident-service.retry-pause:200ms}") Duration retryPause,
            @Value("${resident-service.circuit-breaker.sliding-window-size:10}") int windowSize,
            @Value("${resident-service.circuit-breaker.minimum-calls:5}") int minimumCalls,
            @Value("${resident-service.circuit-breaker.failure-rate-threshold:50}") float failureRate,
            @Value("${resident-service.circuit-breaker.open-duration:10s}") Duration openDuration) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        this.restClient = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.jwtService = jwtService;
        this.retries = retries;
        this.retryPause = retryPause;
        this.circuitBreaker = CircuitBreaker.of("resident-service", CircuitBreakerConfig.custom()
                .slidingWindowSize(windowSize)
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(failureRate)
                .waitDurationInOpenState(openDuration)
                .permittedNumberOfCallsInHalfOpenState(2)
                // A 4xx is a normal answer from a working service.
                .ignoreExceptions(HttpClientErrorException.class)
                .build());
    }

    public List<ApartmentDto> getAllApartments() {
        try {
            List<ApartmentDto> apartments = call(() -> restClient.get()
                    .uri("/apartments")
                    .header("Authorization", "Bearer " + jwtService.issueToken(SERVICE_SUBJECT, null, true))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ApartmentDto>>() {
                    }));
            return apartments != null ? apartments : List.of();
        } catch (RestClientException | CallNotPermittedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not fetch apartments from resident-service", e);
        }
    }

    /**
     * Looks up the logged-in resident by forwarding their own token to resident-service's /auth/me.
     * Empty if resident-service can't be reached, so callers can fall back instead of failing.
     */
    public Optional<ResidentDto> getCurrentResident(String authorizationHeader) {
        try {
            return Optional.ofNullable(call(() -> restClient.get()
                    .uri("/auth/me")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(ResidentDto.class)));
        } catch (RestClientException | CallNotPermittedException e) {
            return Optional.empty();
        }
    }

    /**
     * Apartment number (e.g. "H0101") for an apartment id. Empty if not found or resident-service is down.
     */
    public Optional<String> getApartmentNumber(Long apartmentId) {
        try {
            return getAllApartments().stream()
                    .filter(a -> a.id().equals(apartmentId))
                    .map(ApartmentDto::apartmentNumber)
                    .findFirst();
        } catch (ResponseStatusException e) {
            return Optional.empty();
        }
    }

    /** Runs the call through the circuit breaker, repeating it when it fails in a way that may pass. */
    private <T> T call(Supplier<T> action) {
        Supplier<T> guarded = CircuitBreaker.decorateSupplier(circuitBreaker, action);
        RestClientException failure = null;
        for (int attempt = 0; attempt <= retries; attempt++) {
            try {
                return guarded.get();
            } catch (ResourceAccessException | HttpServerErrorException e) {
                failure = e;
                log.warn("resident-service call failed (attempt {} of {}): {}", attempt + 1, retries + 1, e.getMessage());
                pause(attempt);
            }
            // Anything else (a 4xx, or CallNotPermittedException when the breaker is open) is not repeated.
        }
        throw failure;
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(retryPause.toMillis() * (attempt + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
