package org.conrad.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.rate-limit.login.attempts=3",
        "app.rate-limit.login.period-seconds=60"
})
class LoginRateLimitTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("RESIDENT_URL", () -> "http://127.0.0.1:1");
        registry.add("BILLING_URL", () -> "http://127.0.0.1:1");
        registry.add("MAINTENANCE_URL", () -> "http://127.0.0.1:1");
        registry.add("RESERVATION_URL", () -> "http://127.0.0.1:1");
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:*");
        registry.add("JWT_SECRET", () -> "test-secret-test-secret-test-secret-123456");
    }

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path, String origin) throws Exception {
        return send(method, path, origin, null);
    }

    private HttpResponse<String> send(String method, String path, String origin, String forwardedFor)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json");
        if (origin != null) b.header("Origin", origin);
        if (forwardedFor != null) b.header("X-Forwarded-For", forwardedFor);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void blocksLoginAfterTooManyAttemptsAndOnlyLogin() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(send("POST", "/api/auth/login", null).statusCode()).isNotEqualTo(429);
        }

        HttpResponse<String> blocked = send("POST", "/api/auth/login", "http://localhost:5173");
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();
        assertThat(blocked.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");

        assertThat(send("GET", "/api/residents/household", null).statusCode()).isEqualTo(401);
        assertThat(send("OPTIONS", "/api/auth/login", "http://localhost:5173").statusCode()).isNotEqualTo(429);
    }

    @Test
    void countsAttemptsPerForwardedClientAddressBehindProxy() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(send("POST", "/api/auth/login", null, "203.0.113.7").statusCode()).isNotEqualTo(429);
        }
        assertThat(send("POST", "/api/auth/login", null, "203.0.113.7").statusCode()).isEqualTo(429);
        // A different client behind the same proxy still has its own quota.
        assertThat(send("POST", "/api/auth/login", null, "203.0.113.8").statusCode()).isNotEqualTo(429);
    }
}
