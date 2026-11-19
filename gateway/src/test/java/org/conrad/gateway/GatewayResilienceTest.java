package org.conrad.gateway;

import com.sun.net.httpserver.HttpServer;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** What the gateway does when a service hangs, fails or recovers. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayResilienceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    enum Mode { OK, HANG, FAIL_FIRST_TWO, DROP_CONNECTION }

    private static volatile Mode mode = Mode.OK;
    private static final AtomicInteger calls = new AtomicInteger();
    private static final AtomicInteger inFlight = new AtomicInteger();
    private static final HttpServer stub = startStub();

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            // One thread per request. With a single thread, a hanging call would hold up the requests
            // behind it, and they would be counted later, in the middle of the next test.
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                inFlight.incrementAndGet();
                try {
                    handle(exchange);
                } finally {
                    inFlight.decrementAndGet();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        {
            {
                int n = calls.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                try {
                    if (mode == Mode.HANG) Thread.sleep(4_000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (mode == Mode.DROP_CONNECTION) {
                    exchange.close(); // no answer at all, like a service that crashed mid-request
                    return;
                }
                boolean fail = mode == Mode.FAIL_FIRST_TWO && n <= 2;
                byte[] body = (fail ? "down" : "ok").getBytes(StandardCharsets.UTF_8);
                try {
                    exchange.sendResponseHeaders(fail ? 503 : 200, body.length);
                    exchange.getResponseBody().write(body);
                } catch (IOException ignored) {
                    // the gateway gave up waiting and closed the connection
                }
                exchange.close();
            }
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        for (String name : new String[]{"resident", "billing", "maintenance", "reservation"}) {
            registry.add("spring.cloud.discovery.client.simple.instances." + name + "-service[0].uri",
                    () -> "http://127.0.0.1:" + stub.getAddress().getPort());
        }
        registry.add("JWT_SECRET", () -> SECRET);
        registry.add("spring.http.clients.connect-timeout", () -> "500ms");
        registry.add("spring.http.clients.read-timeout", () -> "700ms");
        // A small window, so the breaker opens after a few failures.
        registry.add("resilience4j.circuitbreaker.configs.default.sliding-window-size", () -> "4");
        registry.add("resilience4j.circuitbreaker.configs.default.minimum-number-of-calls", () -> "4");
        registry.add("resilience4j.circuitbreaker.configs.default.failure-rate-threshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state", () -> "30s");
    }

    @AfterAll
    static void stopStub() {
        stub.stop(0);
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtService jwtService;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void reset() throws InterruptedException {
        mode = Mode.OK;
        // Let calls that the previous test left hanging finish before counting again.
        for (int i = 0; i < 100 && inFlight.get() > 0; i++) Thread.sleep(100);
        calls.set(0);
    }

    private HttpResponse<String> call(String method, String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + jwtService.issueToken("1", "a@b.no", true, 7L))
                .method(method, method.equals("GET") ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString("{}"));
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void aServiceThatHangsIsAbandonedAtTheTimeoutAndAnswersWith503() throws Exception {
        mode = Mode.HANG;

        long start = System.nanoTime();
        HttpResponse<String> response = call("GET", "/api/invoices/overview");
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("billing");
        // 3 attempts at 0.7 s each, well under the 4 s the stub sleeps per call
        assertThat(millis).isLessThan(4_000);
    }

    @Test
    void getRequestsAreRetriedAndSucceedWhenTheServiceRecovers() throws Exception {
        mode = Mode.FAIL_FIRST_TWO;

        HttpResponse<String> response = call("GET", "/api/invoices/overview");

        assertThat(calls.get()).as("calls that reached the service").isEqualTo(3);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ok");
    }

    @Test
    void postRequestsAreNeverRetried() throws Exception {
        mode = Mode.FAIL_FIRST_TWO;

        call("POST", "/api/invoices/generate");

        // Retrying a write could do it twice.
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void afterRepeatedFailuresTheBreakerOpensAndCallsNoLongerReachTheService() throws Exception {
        mode = Mode.DROP_CONNECTION;
        for (int i = 0; i < 4; i++) {
            call("GET", "/api/reservations/me");
        }
        int callsWhenOpen = calls.get();

        HttpResponse<String> response = call("GET", "/api/reservations/me");

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("reservation");
        assertThat(calls.get()).as("an open breaker doesn't call the service").isEqualTo(callsWhenOpen);
    }
}
