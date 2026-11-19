package org.conrad.reservationservice;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.model.ReservationStatus;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.model.ResourceType;
import org.conrad.reservationservice.repository.ReservationRepository;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.conrad.reservationservice.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Needs the Postgres from docker-compose (the constraint is Postgres-specific).
@SpringBootTest
class DoubleBookingTest {

    @Autowired ReservationService service;
    @Autowired ReservationRepository reservations;
    @Autowired ResourceRepository resources;

    Resource resource;
    LocalDateTime start;

    @BeforeEach
    void setUp() {
        resource = resources.save(Resource.builder()
                .name("Test room").type(ResourceType.COMMON_ROOM).status(ResourceStatus.AVAILABLE).build());
        start = LocalDateTime.now().plusDays(30).withMinute(0).withSecond(0).withNano(0);
    }

    @AfterEach
    void tearDown() {
        reservations.deleteAll(reservations.findByResourceIdAndStatus(resource.getId(), ReservationStatus.ACTIVE));
        reservations.deleteAll(reservations.findByResourceIdAndStatus(resource.getId(), ReservationStatus.CANCELLED));
        resources.delete(resource);
    }

    private Reservation raw(LocalDateTime from, LocalDateTime to) {
        return Reservation.builder().resource(resource).residentId(1L)
                .startTime(from).endTime(to).status(ReservationStatus.ACTIVE).build();
    }

    @Test
    void databaseRejectsOverlappingActiveReservationsEvenWithoutTheServiceCheck() {
        reservations.saveAndFlush(raw(start, start.plusHours(2)));

        assertThatThrownBy(() -> reservations.saveAndFlush(raw(start.plusHours(1), start.plusHours(3))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void backToBackBookingsAreAllowed() {
        reservations.saveAndFlush(raw(start, start.plusHours(1)));
        reservations.saveAndFlush(raw(start.plusHours(1), start.plusHours(2)));

        assertThat(reservations.findByResourceIdAndStatus(resource.getId(), ReservationStatus.ACTIVE)).hasSize(2);
    }

    @Test
    void cancelledReservationFreesTheSlot() {
        Reservation first = service.reserve(resource.getId(), 1L, start, start.plusHours(1));
        service.cancel(first.getId(), 1L);

        service.reserve(resource.getId(), 2L, start, start.plusHours(1));
    }

    // Repeated because it is a race: with a single run, Postgres sometimes aborts a request with a
    // deadlock instead of a constraint violation, and that used to surface as a 500.
    @RepeatedTest(5)
    void concurrentBookingsOfTheSameSlotLetExactlyOneThrough() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Future<Boolean>[] results = new Future[threads];
            for (int i = 0; i < threads; i++) {
                long residentId = i + 1;
                Callable<Boolean> attempt = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        service.reserve(resource.getId(), residentId, start, start.plusHours(1));
                        return true;
                    } catch (ResponseStatusException e) {
                        assertThat(e.getStatusCode().value()).isEqualTo(409);
                        return false;
                    }
                };
                results[i] = pool.submit(attempt);
            }
            ready.await();
            go.countDown();

            int succeeded = 0;
            for (Future<Boolean> f : results) {
                if (f.get()) succeeded++;
            }
            assertThat(succeeded).isEqualTo(1);
            assertThat(reservations.findByResourceIdAndStatus(resource.getId(), ReservationStatus.ACTIVE)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
