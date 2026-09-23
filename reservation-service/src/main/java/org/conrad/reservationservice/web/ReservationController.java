package org.conrad.reservationservice.web;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<Reservation> reserve(@RequestBody ReserveRequest request) {
        Reservation reservation = reservationService.reserve(
                request.resourceId(),
                request.residentId(),
                request.startTime(),
                request.endTime()
        );
        return ResponseEntity.ok(reservation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Reservation> cancel(@PathVariable Long id, @RequestParam Long residentId) {
        Reservation cancelled = reservationService.cancel(id, residentId);
        return ResponseEntity.ok(cancelled);
    }

    public record ReserveRequest(
            Long resourceId,
            Long residentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }
}
//TODO: residentId skal hentes fra JWT via SecurityContextHolder når security-laget er på plass
//TODO: bytt ReserveRequest til en egen DTO i dto-pakken
//TODO: returner en ReservationResponse i stedet for Reservation-entiteten direkte