package org.conrad.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filter only decides who the caller is. It never rejects a request itself: an invalid or
 * missing token leaves the request anonymous, and the services' security rules answer 401/403.
 * So every test also checks that the chain continues.
 */
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!";

    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- valid tokens and roles -------------------------------------------------------------

    @Test
    void residentTokenGivesOnlyTheResidentRole() throws Exception {
        authorize(jwtService.issueToken("7", "kari@example.com", false, 3L));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("7");
        assertThat(roles(auth)).containsExactly("ROLE_RESIDENT");
        assertChainContinued();
    }

    @Test
    void boardTokenGivesBoardAndResidentRoles() throws Exception {
        authorize(jwtService.issueToken("1", "board@example.com", true, 1L));

        assertThat(roles(SecurityContextHolder.getContext().getAuthentication()))
                .containsExactlyInAnyOrder("ROLE_BOARD", "ROLE_RESIDENT");
        assertChainContinued();
    }

    @Test
    void workerTokenGivesOnlyTheWorkerRole() throws Exception {
        authorize(jwtService.issueWorkerToken(4L, "per@example.com"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("worker:4");
        assertThat(roles(auth)).containsExactly("ROLE_WORKER");
        assertChainContinued();
    }

    @Test
    void theClaimsAreAvailableToTheServices() throws Exception {
        authorize(jwtService.issueToken("7", "kari@example.com", false, 3L));

        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertThat(claims.get("email", String.class)).isEqualTo("kari@example.com");
        assertThat(claims.get("apartmentId", Long.class)).isEqualTo(3L);
    }

    // --- no or unusable credentials: anonymous, chain continues --------------------------------

    @Test
    void noAuthorizationHeaderLeavesTheRequestAnonymous() throws Exception {
        filter.doFilter(request, response, chain);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void aHeaderThatIsNotBearerIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic a2FyaTpwdw==");
        filter.doFilter(request, response, chain);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void bearerInLowerCaseIsNotAccepted() throws Exception {
        // The filter matches "Bearer " exactly. This pins down that behaviour.
        request.addHeader("Authorization", "bearer " + jwtService.issueToken("7", "a@b.no", false));
        filter.doFilter(request, response, chain);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void anEmptyBearerTokenIsIgnored() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        filter.doFilter(request, response, chain);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void aMalformedTokenIsIgnored() throws Exception {
        request.addHeader("Authorization", "Bearer this-is-not-a-jwt");
        filter.doFilter(request, response, chain);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void anExpiredTokenIsIgnored() throws Exception {
        // A negative lifetime gives a token that has already expired.
        String expired = new JwtService(SECRET, -5).issueToken("7", "kari@example.com", false);

        authorize(expired);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void aTokenSignedWithAnotherSecretIsIgnored() throws Exception {
        String forged = new JwtService("another-secret-that-is-also-32-bytes-long", 60)
                .issueToken("1", "board@example.com", true);

        authorize(forged);

        assertAnonymous();
        assertChainContinued();
    }

    @Test
    void aTokenWhoseClaimsWereChangedIsIgnored() throws Exception {
        // Swap the payload of a resident token for one that claims manager=true, keeping the old
        // signature. The signature no longer matches, so it must be rejected.
        String resident = jwtService.issueToken("7", "kari@example.com", false);
        String board = jwtService.issueToken("7", "kari@example.com", true);
        String[] r = resident.split("\\.");
        String[] b = board.split("\\.");
        String tampered = r[0] + "." + b[1] + "." + r[2];

        authorize(tampered);

        assertAnonymous();
        assertChainContinued();
    }

    // --- an earlier request must not leak into this one ---------------------------------------

    @Test
    void anInvalidTokenClearsAnAuthenticationThatWasAlreadySet() throws Exception {
        authorize(jwtService.issueToken("7", "kari@example.com", false));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer garbage");
        filter.doFilter(request, response, new MockFilterChain());

        assertAnonymous();
    }

    // --- helpers ------------------------------------------------------------------------------

    private void authorize(String token) throws Exception {
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, response, chain);
    }

    private static List<String> roles(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    }

    private void assertAnonymous() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void assertChainContinued() {
        assertThat(chain.getRequest()).as("the request must be passed on").isNotNull();
        assertThat(response.getStatus()).as("the filter must not answer itself").isEqualTo(200);
    }
}
