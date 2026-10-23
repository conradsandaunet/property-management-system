package org.conrad.gateway;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    /** A recorded request as seen by a stub service. */
    record Seen(String service, String method, String path, String authorization, String body) {}

    private static final List<Seen> seen = new CopyOnWriteArrayList<>();
    private static final Map<String, HttpServer> stubs = Map.of(
            "resident", startStub("resident"),
            "billing", startStub("billing"),
            "maintenance", startStub("maintenance"),
            "reservation", startStub("reservation"));

    private static HttpServer startStub(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                seen.add(new Seen(name, exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"), body));
                byte[] response = ("from-" + name).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String url(String name) {
        return "http://127.0.0.1:" + stubs.get(name).getAddress().getPort();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("RESIDENT_URL", () -> url("resident"));
        registry.add("BILLING_URL", () -> url("billing"));
        registry.add("MAINTENANCE_URL", () -> url("maintenance"));
        registry.add("RESERVATION_URL", () -> url("reservation"));
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:*");
        registry.add("JWT_SECRET", () -> SECRET);
    }

    @AfterAll
    static void stopStubs() {
        stubs.values().forEach(s -> s.stop(0));
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtService jwtService;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clear() {
        seen.clear();
    }

    private HttpResponse<String> send(String method, String path, String token, String body, String... headers)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) b.header("Authorization", "Bearer " + token);
        for (int i = 0; i < headers.length; i += 2) b.header(headers[i], headers[i + 1]);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String validToken() {
        return jwtService.issueToken("1", "a@b.no", false, 7L);
    }

    // ---- routing ----

    @Test
    void routesEachPathGroupToItsServiceAndStripsApiPrefix() throws Exception {
        Map<String, String[]> cases = Map.of(
                "/api/auth/me", new String[]{"resident", "/auth/me"},
                "/api/residents/search", new String[]{"resident", "/residents/search"},
                "/api/apartments/1", new String[]{"resident", "/apartments/1"},
                "/api/invoices/current", new String[]{"billing", "/invoices/current"},
                "/api/maintenance/my", new String[]{"maintenance", "/maintenance/my"},
                "/api/workers", new String[]{"maintenance", "/workers"},
                "/api/reservations/my", new String[]{"reservation", "/reservations/my"},
                "/api/resources", new String[]{"reservation", "/resources"});

        for (var entry : cases.entrySet()) {
            seen.clear();
            HttpResponse<String> response = send("GET", entry.getKey(), validToken(), null);
            assertThat(response.statusCode()).as(entry.getKey()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("from-" + entry.getValue()[0]);
            assertThat(seen).hasSize(1);
            assertThat(seen.get(0).path()).isEqualTo(entry.getValue()[1]);
        }
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        assertThat(send("GET", "/api/nothing-here", validToken(), null).statusCode()).isEqualTo(404);
        assertThat(seen).isEmpty();
    }

    @Test
    void forwardsMethodBodyAndAuthorizationUnchanged() throws Exception {
        String token = validToken();
        for (String method : List.of("POST", "PATCH", "DELETE")) {
            seen.clear();
            send(method, "/api/residents/5", token, "{\"x\":1}", "Content-Type", "application/json");
            assertThat(seen).hasSize(1);
            assertThat(seen.get(0).method()).isEqualTo(method);
            assertThat(seen.get(0).authorization()).isEqualTo("Bearer " + token);
            assertThat(seen.get(0).body()).isEqualTo("{\"x\":1}");
        }
    }

    // ---- JWT ----

    @Test
    void loginIsOpenWithoutToken() throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/login", null, "{}",
                "Content-Type", "application/json");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(seen).hasSize(1);
    }

    @Test
    void otherRoutesRequireToken() throws Exception {
        assertThat(send("GET", "/api/residents/household", null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/auth/login", null, null).statusCode()).isEqualTo(401);
        assertThat(seen).isEmpty();
    }

    @Test
    void rejectsBadTokensWith401AndNeverReachesService() throws Exception {
        SecretKey wrongKey = Keys.hmacShaKeyFor("another-secret-another-secret-another-1234".getBytes());
        SecretKey rightKey = Keys.hmacShaKeyFor(SECRET.getBytes());
        Instant now = Instant.now();

        String expired = Jwts.builder().subject("1")
                .issuedAt(Date.from(now.minusSeconds(7200))).expiration(Date.from(now.minusSeconds(3600)))
                .signWith(rightKey).compact();
        String wrongSignature = Jwts.builder().subject("1")
                .expiration(Date.from(now.plusSeconds(3600))).signWith(wrongKey).compact();

        String[] parts = validToken().split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"manager\":false", "\"manager\":true");
        String tampered = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String algNone = enc.encodeToString("{\"alg\":\"none\"}".getBytes()) + "."
                + enc.encodeToString("{\"sub\":\"1\",\"manager\":true}".getBytes()) + ".";

        for (String token : List.of("garbage", expired, wrongSignature, tampered, algNone)) {
            HttpResponse<String> response = send("GET", "/api/residents/household", token, null);
            assertThat(response.statusCode()).as(token).isEqualTo(401);
        }
        assertThat(seen).isEmpty();
    }

    // ---- CORS ----

    @Test
    void preflightWithoutTokenFromAllowedOriginSucceeds() throws Exception {
        HttpResponse<String> response = send("OPTIONS", "/api/residents/1", null, null,
                "Origin", ALLOWED_ORIGIN,
                "Access-Control-Request-Method", "PATCH",
                "Access-Control-Request-Headers", "authorization,content-type");
        assertThat(response.statusCode()).isBetween(200, 204);
        assertThat(response.headers().allValues("Access-Control-Allow-Origin")).containsExactly(ALLOWED_ORIGIN);
        assertThat(response.headers().firstValue("Access-Control-Allow-Methods").orElse("")).contains("PATCH");
        assertThat(seen).isEmpty();
    }

    @Test
    void preflightFromOtherOriginIsRejected() throws Exception {
        HttpResponse<String> response = send("OPTIONS", "/api/residents/1", null, null,
                "Origin", "http://evil.example",
                "Access-Control-Request-Method", "PATCH");
        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void proxiedResponseHasExactlyOneAllowOriginHeader() throws Exception {
        HttpResponse<String> response = send("GET", "/api/invoices/current", validToken(), null,
                "Origin", ALLOWED_ORIGIN);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("Access-Control-Allow-Origin")).containsExactly(ALLOWED_ORIGIN);
    }

    // ---- downstream down ----

    @Test
    void downstreamDownGivesServerError() throws Exception {
        stubs.get("reservation").stop(0);
        HttpResponse<String> response = send("GET", "/api/resources", validToken(), null);
        assertThat(response.statusCode()).isIn(500, 502, 503, 504);
    }
}
