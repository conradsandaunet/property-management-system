package org.conrad.reservationservice.config;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.model.ReservationStatus;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.model.ResourceType;
import org.conrad.reservationservice.repository.ReservationRepository;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Demo resources (parking spaces, common rooms) and a few upcoming bookings. residentId refers to
 * residents seeded by resident-service.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoReservationSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoReservationSeed.class);

    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;

    public DemoReservationSeed(ResourceRepository resourceRepository, ReservationRepository reservationRepository) {
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (resourceRepository.count() > 0) {
            return;
        }

        Resource p1 = resource("Parkeringsplass P1", ResourceType.PARKING);
        Resource p2 = resource("Parkeringsplass P2", ResourceType.PARKING);
        Resource p3 = resource("Parkeringsplass P3", ResourceType.PARKING);
        Resource commonRoom = resource("Fellesrom", ResourceType.COMMON_ROOM);
        Resource roofTerrace = resource("Takterrasse", ResourceType.COMMON_ROOM);

        // The terrace is closed for repairs for three days next week.
        LocalDateTime nextWeek = LocalDateTime.now().plusDays(7).with(LocalTime.MIDNIGHT);
        roofTerrace.setMaintenanceFrom(nextWeek);
        roofTerrace.setMaintenanceTo(nextWeek.plusDays(3));

        resourceRepository.save(p1);
        resourceRepository.save(p2);
        resourceRepository.save(p3);
        resourceRepository.save(commonRoom);
        resourceRepository.save(roofTerrace);

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).with(LocalTime.MIDNIGHT);
        reservationRepository.save(booking(commonRoom, 2L, tomorrow.plusHours(18), tomorrow.plusHours(21)));
        reservationRepository.save(booking(commonRoom, 5L, tomorrow.plusDays(2).plusHours(12), tomorrow.plusDays(2).plusHours(16)));
        reservationRepository.save(booking(p1, 3L, tomorrow.plusHours(8), tomorrow.plusHours(17)));
        reservationRepository.save(booking(p2, 6L, tomorrow.plusDays(1).plusHours(9), tomorrow.plusDays(1).plusHours(12)));

        log.info("Seeded 5 demo resources and 4 demo reservations");
    }

    private Resource resource(String name, ResourceType type) {
        return Resource.builder().name(name).type(type).status(ResourceStatus.AVAILABLE).build();
    }

    private Reservation booking(Resource resource, Long residentId, LocalDateTime start, LocalDateTime end) {
        return Reservation.builder()
                .resource(resource)
                .residentId(residentId)
                .startTime(start)
                .endTime(end)
                .status(ReservationStatus.ACTIVE)
                .build();
    }
}
