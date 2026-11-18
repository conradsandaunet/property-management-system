package org.conrad.reservationservice;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.model.ReservationStatus;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.model.ResourceType;
import org.conrad.reservationservice.repository.ReservationRepository;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: real HTTP requests through security, controller, service and the Postgres database,
 * each signed with a real JWT like the gateway forwards. Needs the reservation-db from docker-compose.
 * Each test follows a user story as Given / When / Then.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationApiTest {

    // High ids so the tests never touch real or seeded residents.
    private static final long ALICE = 990_001L;
    private static final long BOB = 990_002L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired ResourceRepository resources;
    @Autowired ReservationRepository reservations;

    Resource room;
    LocalDate day;

    @BeforeEach
    void setUp() {
        room = resources.save(Resource.builder()
                .name("Test room").type(ResourceType.COMMON_ROOM).status(ResourceStatus.AVAILABLE).build());
        day = LocalDate.now().plusDays(30);
    }

    @AfterEach
    void tearDown() {
        reservations.deleteAll(reservations.findByResidentId(ALICE));
        reservations.deleteAll(reservations.findByResidentId(BOB));
        resources.delete(room);
    }

    private String resident(long id) {
        return "Bearer " + jwt.issueToken(String.valueOf(id), "resident@test", false, 1L);
    }

    private String board() {
        return "Bearer " + jwt.issueToken("990999", "board@test", true, 1L);
    }

    private Reservation book(long residentId, int hour) {
        return reservations.saveAndFlush(Reservation.builder()
                .resource(room).residentId(residentId)
                .startTime(day.atTime(hour, 0)).endTime(day.atTime(hour + 1, 0))
                .status(ReservationStatus.ACTIVE).build());
    }

    @Test
    @DisplayName("A resident sees only their own reservations")
    void residentSeesOnlyOwnReservations() throws Exception {
        // Given Alice and Bob each have a booking
        Reservation alicesBooking = book(ALICE, 10);
        book(BOB, 12);

        // When Alice asks for her reservations
        // Then she gets her own booking and not Bob's
        mvc.perform(get("/reservations/me").header("Authorization", resident(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(alicesBooking.getId()))
                .andExpect(jsonPath("$[*].residentId", everyItem(is((int) ALICE))));
    }

    @Test
    @DisplayName("A resident cannot cancel someone else's reservation")
    void residentCannotCancelSomeoneElsesReservation() throws Exception {
        // Given Bob has a booking
        Reservation bobsBooking = book(BOB, 10);

        // When Alice tries to cancel it
        mvc.perform(delete("/reservations/{id}", bobsBooking.getId()).header("Authorization", resident(ALICE)))
                // Then she is refused, and Bob's booking is still active
                .andExpect(status().isForbidden());

        assertThat(reservations.findById(bobsBooking.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("When the board blocks a period, residents see those slots as taken and can't book them")
    void boardBlockStopsBookingsInThatPeriod() throws Exception {
        // Given the board blocks the room from 10:00 to 12:00
        mvc.perform(post("/resources/{id}/block", room.getId())
                        .header("Authorization", board())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from": "%sT10:00:00", "to": "%sT12:00:00"}""".formatted(day, day)))
                .andExpect(status().isOk());

        // When a resident looks at availability that day
        // Then 10:00 and 11:00 are unavailable, while 09:00 and 12:00 are still free
        mvc.perform(get("/resources/{id}/availability", room.getId())
                        .param("date", day.toString())
                        .header("Authorization", resident(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.startTime == '%sT09:00:00')].available".formatted(day)).value(true))
                .andExpect(jsonPath("$[?(@.startTime == '%sT10:00:00')].available".formatted(day)).value(false))
                .andExpect(jsonPath("$[?(@.startTime == '%sT11:00:00')].available".formatted(day)).value(false))
                .andExpect(jsonPath("$[?(@.startTime == '%sT12:00:00')].available".formatted(day)).value(true));

        // And trying to book 10:00 anyway is rejected, with nothing saved
        mvc.perform(post("/reservations")
                        .header("Authorization", resident(ALICE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resourceId": %d, "startTime": "%sT10:00:00", "endTime": "%sT11:00:00"}"""
                                .formatted(room.getId(), day, day)))
                .andExpect(status().isConflict());

        assertThat(reservations.findByResidentId(ALICE)).isEmpty();
    }

    @Test
    @DisplayName("Only the board can block a resource")
    void residentCannotBlockAResource() throws Exception {
        mvc.perform(post("/resources/{id}/block", room.getId())
                        .header("Authorization", resident(ALICE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from": "%sT10:00:00", "to": "%sT12:00:00"}""".formatted(day, day)))
                .andExpect(status().isForbidden());

        assertThat(resources.findById(room.getId()).orElseThrow().getMaintenanceFrom()).isNull();
    }
}
