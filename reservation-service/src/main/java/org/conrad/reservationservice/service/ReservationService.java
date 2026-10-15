package org.conrad.reservationservice.service;

import org.conrad.reservationservice.dto.SlotResponse;
import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.model.ReservationStatus;
import org.conrad.reservationservice.model.Resource;
import org.conrad.reservationservice.model.ResourceStatus;
import org.conrad.reservationservice.repository.ReservationRepository;
import org.conrad.reservationservice.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {
    // Opening hours used when listing available slots (one-hour slots)
    private static final LocalTime FIRST_SLOT = LocalTime.of(8, 0);
    private static final LocalTime LAST_SLOT_END = LocalTime.of(22, 0);

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;

    public ReservationService(ReservationRepository reservationRepository, ResourceRepository resourceRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
    }

    public Reservation reserve(Long resourceId, Long residentId, LocalDateTime start, LocalDateTime end) {
        if (resourceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resourceId is required");
        }
        validateTimes(start, end);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        if (resource.getStatus() == ResourceStatus.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource is under maintenance and cannot be booked");
        }

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

    public List<Reservation> listForResource(Long resourceId) {
        resourceRepository.findById(resourceId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        return reservationRepository.findByResourceIdAndStatus(resourceId, ReservationStatus.ACTIVE);
    }


    /**
     * One-hour slots for the given day, marked available or not. Everything is unavailable during maintenance.
     */
    public List<SlotResponse> availableSlots(Long resourceId, LocalDate date) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        boolean underMaintenance = resource.getStatus() == ResourceStatus.MAINTENANCE;
        List<Reservation> booked = reservationRepository.findByResourceIdAndStatus(resourceId, ReservationStatus.ACTIVE);

        List<SlotResponse> slots = new ArrayList<>();
        for (LocalTime time = FIRST_SLOT; time.isBefore(LAST_SLOT_END); time = time.plusHours(1)) {
            LocalDateTime slotStart = date.atTime(time);
            LocalDateTime slotEnd = slotStart.plusHours(1);

            boolean taken = booked.stream().anyMatch(r ->
                    r.getStartTime().isBefore(slotEnd) && r.getEndTime().isAfter(slotStart));
            boolean inPast = slotStart.isBefore(LocalDateTime.now());

            slots.add(new SlotResponse(slotStart, slotEnd, !underMaintenance && !taken && !inPast));
        }
        return slots;
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime are required");
        }
        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime must be before endTime");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book a time in the past");
        }
    }
}




