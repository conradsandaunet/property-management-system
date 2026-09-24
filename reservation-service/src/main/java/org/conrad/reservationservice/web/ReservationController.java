package org.conrad.reservationservice.web;

import org.conrad.reservationservice.dto.ReservationCreateRequest;
import org.conrad.reservationservice.dto.ReservationResponse;
import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@RequestBody ReservationCreateRequest request) {
        Reservation reservation = reservationService.reserve(
                request.resourceId(),
                request.residentId(),
                request.startTime(),
                request.endTime()
        );
        return ResponseEntity.ok(toResponse(reservation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long id,
            @RequestParam Long residentId) {
        Reservation cancelled = reservationService.cancel(id, residentId);
        return ResponseEntity.ok(toResponse(cancelled));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> listForResource(@RequestParam Long resourceId) {
        List<ReservationResponse> responses = reservationService.listForResource(resourceId)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getResidentId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus()
        );
    }

}
//TODO: residentId skal hentes fra JWT via SecurityContextHolder når security-laget er på plass
