package org.conrad.gateway;

import com.sun.net.httpserver.HttpServer;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A service that is slow but working must not be cut off. Resilience4j's time limiter defaults to
 * one second and would answer 503 for anything slower (and count it as a failure, which opens the
 * breaker). Cold services, PDF generation and photo uploads all take longer than that. The only limit
 * is the HTTP client's read timeout.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySlowResponseTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private static final HttpServer stub = startStub();

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        for (String name : new String[]{"resident", "billing", "maintenance", "reservation"}) {
            registry.add("spring.cloud.discovery.client.simple.instances." + name + "-service[0].uri",
                    () -> "http://127.0.0.1:" + stub.getAddress().getPort());
        }
        registry.add("JWT_SECRET", () -> SECRET);
        registry.add("spring.http.clients.read-timeout", () -> "6s");
    }

    @AfterAll
    static void stopStub() {
        stub.stop(0);
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtService jwtService;

    @Test
    void aSlowButWorkingServiceIsNotCutOffByTheBreaker() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/resources"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + jwtService.issueToken("1", "a@b.no", true, 7L))
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).as("the service answers after 2 s").isEqualTo(200);
        assertThat(response.body()).isEqualTo("ok");
    }
}
