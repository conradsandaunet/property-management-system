package org.conrad.residentservice.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String name) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(name, null, List.of()));
    }

    @Test
    void name_withoutAuthentication_isUnknown() {
        assertThat(CurrentUser.name()).isEqualTo("unknown");
    }

    @Test
    void name_returnsAuthenticatedPrincipalName() {
        authenticateAs("42");

        assertThat(CurrentUser.name()).isEqualTo("42");
    }

    @Test
    void requireResidentId_parsesNumericSubject() {
        authenticateAs("42");

        assertThat(CurrentUser.requireResidentId()).isEqualTo(42L);
    }

    @Test
    void requireResidentId_serviceTokenSubject_throwsForbidden() {
        authenticateAs("billing-service");

        assertThatThrownBy(CurrentUser::requireResidentId)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void requireResidentId_withoutAuthentication_throwsForbidden() {
        assertThatThrownBy(CurrentUser::requireResidentId)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()));
    }
}
