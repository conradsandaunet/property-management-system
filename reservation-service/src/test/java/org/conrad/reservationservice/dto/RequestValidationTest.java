package org.conrad.reservationservice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Required fields. Rules that depend on the data (start before end, not in the past) stay in the service. */
class RequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 1, 1, 10, 0);

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void aBookingNeedsResourceStartAndEnd() {
        assertThat(invalidFields(new ReservationCreateRequest(null, null, null)))
                .containsExactlyInAnyOrder("resourceId", "startTime", "endTime");
        assertThat(invalidFields(new ReservationCreateRequest(1L, NOW, NOW.plusHours(1)))).isEmpty();
    }

    @Test
    void aBlockNeedsFromAndTo() {
        assertThat(invalidFields(new BlockResourceRequest(null, null)))
                .containsExactlyInAnyOrder("from", "to");
        assertThat(invalidFields(new BlockResourceRequest(NOW, NOW.plusDays(1)))).isEmpty();
    }
}
