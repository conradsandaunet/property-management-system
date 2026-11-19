package org.conrad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void aResidentTokenCarriesTheClaimsItWasIssuedWith() {
        Claims claims = jwtService.validateAndParse(
                jwtService.issueToken("7", "kari@example.com", false, 3L));

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("email", String.class)).isEqualTo("kari@example.com");
        assertThat(claims.get("manager", Boolean.class)).isFalse();
        assertThat(claims.get("apartmentId", Long.class)).isEqualTo(3L);
        assertThat(claims.get("role")).isNull();
    }

    @Test
    void aWorkerTokenHasANonNumericSubjectAndTheWorkerRole() {
        Claims claims = jwtService.validateAndParse(jwtService.issueWorkerToken(4L, "per@example.com"));

        // The prefix keeps a worker id from ever being read as a resident id.
        assertThat(claims.getSubject()).isEqualTo(JwtService.WORKER_SUBJECT_PREFIX + "4");
        assertThat(claims.get("role", String.class)).isEqualTo(JwtService.WORKER_ROLE);
        assertThat(claims.get("manager", Boolean.class)).isFalse();
    }

    @Test
    void theExpirationFollowsTheConfiguredMinutes() {
        long before = System.currentTimeMillis();
        Claims claims = new JwtService(SECRET, 15)
                .validateAndParse(new JwtService(SECRET, 15).issueToken("7", "a@b.no", false));

        long minutes = (claims.getExpiration().getTime() - before) / 60_000;
        assertThat(minutes).isBetween(14L, 15L);
        assertThat(claims.getIssuedAt()).isBeforeOrEqualTo(new Date());
    }

    @Test
    void anExpiredTokenIsRejected() {
        String expired = new JwtService(SECRET, -1).issueToken("7", "a@b.no", false);

        assertThatThrownBy(() -> jwtService.validateAndParse(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenFromAnotherSecretIsRejected() {
        String other = new JwtService("another-secret-that-is-also-32-bytes-long", 60)
                .issueToken("7", "a@b.no", false);

        assertThatThrownBy(() -> jwtService.validateAndParse(other)).isInstanceOf(JwtException.class);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> jwtService.validateAndParse("not-a-token")).isInstanceOf(JwtException.class);
    }
}
