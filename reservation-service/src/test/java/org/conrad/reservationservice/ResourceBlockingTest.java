package org.conrad.reservationservice;

import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.model.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test (no Spring, no database) for the rule that decides whether a resource can be booked.
 * User story: as a board member I want to block a resource for a period, so residents can't book it then.
 */
class ResourceBlockingTest {

    private static final LocalDateTime BLOCK_FROM = LocalDateTime.of(2030, 6, 10, 12, 0);
    private static final LocalDateTime BLOCK_TO = LocalDateTime.of(2030, 6, 10, 16, 0);

    private Resource blockedResource() {
        return Resource.builder()
                .name("Common room").type(ResourceType.COMMON_ROOM).status(ResourceStatus.AVAILABLE)
                .maintenanceFrom(BLOCK_FROM).maintenanceTo(BLOCK_TO)
                .build();
    }

    @Test
    @DisplayName("A booking that overlaps the blocked period, even partly, is blocked")
    void overlappingBookingIsBlocked() {
        Resource resource = blockedResource();

        assertThat(resource.isBlockedDuring(BLOCK_FROM.minusHours(1), BLOCK_FROM.plusHours(1))).isTrue();
        assertThat(resource.isBlockedDuring(BLOCK_TO.minusHours(1), BLOCK_TO.plusHours(1))).isTrue();
        assertThat(resource.isBlockedDuring(BLOCK_FROM.minusHours(1), BLOCK_TO.plusHours(1))).isTrue();
    }

    @Test
    @DisplayName("Bookings that end when the block starts, or start when it ends, are allowed")
    void bookingsTouchingTheBlockAreAllowed() {
        Resource resource = blockedResource();

        assertThat(resource.isBlockedDuring(BLOCK_FROM.minusHours(1), BLOCK_FROM)).isFalse();
        assertThat(resource.isBlockedDuring(BLOCK_TO, BLOCK_TO.plusHours(1))).isFalse();
    }

    @Test
    @DisplayName("A resource closed until further notice is blocked at any time, with or without a period")
    void closedResourceIsAlwaysBlocked() {
        Resource closed = Resource.builder()
                .name("Guest parking").type(ResourceType.PARKING).status(ResourceStatus.MAINTENANCE)
                .build();

        assertThat(closed.isBlockedDuring(BLOCK_TO.plusDays(100), BLOCK_TO.plusDays(100).plusHours(1))).isTrue();
    }
}
