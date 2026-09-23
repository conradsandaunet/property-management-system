package org.conrad.reservationservice.service;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.model.ReservationStatus;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.repository.ReservationRepository;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;

    public ReservationService(ReservationRepository reservationRepository, ResourceRepository resourceRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
    }

    public Reservation reserve(Long resourceId, Long residentId, LocalDateTime start, LocalDateTime end) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        boolean overlaps = reservationRepository.existsOverlapping(resourceId, start, end);
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Time slot already booked");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .residentId(residentId)
                .startTime(start)
                .endTime(end)
                .status(ReservationStatus.ACTIVE)
                .build();

        return reservationRepository.save(reservation);

    }

    public Reservation cancel(Long reservationId, Long residentId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        if (!reservation.getResidentId().equals(residentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own reservations");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is already cancelled");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }
}
