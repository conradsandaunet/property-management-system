package org.conrad.billingservice.client;

import com.sun.net.httpserver.HttpServer;
import org.conrad.billingservice.dto.ApartmentDto;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** How billing behaves when resident-service is slow, failing or gone. No database or Spring needed. */
class ResidentServiceClientTest {

    private enum Mode { OK, HANG, FAIL_FIRST_TWO, ALWAYS_500, NOT_FOUND }

    private HttpServer stub;
    private volatile Mode mode;
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void startStub() throws IOException {
        mode = Mode.OK;
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/", exchange -> {
            int n = calls.incrementAndGet();
            try {
                if (mode == Mode.HANG) Thread.sleep(2_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            int status = switch (mode) {
                case ALWAYS_500 -> 500;
                case FAIL_FIRST_TWO -> n <= 2 ? 503 : 200;
                case NOT_FOUND -> 404;
                default -> 200;
            };
            byte[] body = (status == 200 ? "[{\"id\":1,\"apartmentNumber\":\"101\"}]" : "{}")
                    .getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
                // the client gave up waiting
            }
            exchange.close();
        });
        // One thread per request, so a hanging call doesn't queue up the next attempt behind it.
        stub.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        stub.start();
    }

    @AfterEach
    void stopStub() {
        stub.stop(0);
    }

    private ResidentServiceClient client(int retries) {
        return new ResidentServiceClient(RestClient.builder(),
                new JwtService("test-secret-test-secret-test-secret-123456", 60),
                "http://127.0.0.1:" + stub.getAddress().getPort(),
                Duration.ofMillis(300), Duration.ofMillis(400), retries, Duration.ofMillis(10),
                // window of 4, trips at 50 % once 4 calls are recorded, stays open for a minute
                4, 4, 50f, Duration.ofSeconds(60));
    }

    @Test
    void returnsTheApartmentsWhenTheServiceAnswers() {
        List<ApartmentDto> apartments = client(2).getAllApartments();

        assertThat(apartments).extracting(ApartmentDto::apartmentNumber).containsExactly("101");
    }

    @Test
    void aServiceThatHangsIsAbandonedAtTheReadTimeoutAndGivesBadGateway() {
        mode = Mode.HANG;
        ResidentServiceClient client = client(1);

        long start = System.nanoTime();
        assertThatThrownBy(client::getAllApartments)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        long millis = (System.nanoTime() - start) / 1_000_000;

        // two attempts of 0.4 s each, not the 2 s the stub sleeps per call
        assertThat(millis).isLessThan(1_800);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void aFailureThatPassesIsRetried() {
        mode = Mode.FAIL_FIRST_TWO;

        List<ApartmentDto> apartments = client(2).getAllApartments();

        assertThat(apartments).hasSize(1);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void aRealAnswerLikeNotFoundIsNotRetried() {
        mode = Mode.NOT_FOUND;

        // getApartmentNumber turns any failure into "unknown"
        assertThat(client(2).getApartmentNumber(1L)).isEmpty();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void afterRepeatedFailuresTheBreakerOpensAndTheServiceIsLeftAlone() {
        mode = Mode.ALWAYS_500;
        ResidentServiceClient client = client(0);
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(client::getAllApartments).isInstanceOf(ResponseStatusException.class);
        }
        int callsWhenOpen = calls.get();

        long start = System.nanoTime();
        assertThatThrownBy(client::getAllApartments)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));

        assertThat(calls.get()).as("an open breaker doesn't call the service").isEqualTo(callsWhenOpen);
        assertThat((System.nanoTime() - start) / 1_000_000).as("it fails at once").isLessThan(100);
    }

    @Test
    void theCurrentResidentIsEmptyWhenTheServiceIsDown() {
        stub.stop(0);

        assertThat(client(0).getCurrentResident("Bearer x")).isEmpty();
    }
}
