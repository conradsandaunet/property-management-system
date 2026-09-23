package org.conrad.reservationservice.web;

import org.conrad.reservationservice.dto.ReservationCreateRequest;
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
    public ResponseEntity<Reservation> reserve(@RequestBody ReservationCreateRequest request) {
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

    @GetMapping
    public ResponseEntity<List<Reservation>> listForResource(@RequestParam Long resourceId) {
        List<Reservation> reservations = reservationService.listForResource(resourceId);
        return ResponseEntity.ok(reservations);
    }

}
//TODO: residentId skal hentes fra JWT via SecurityContextHolder når security-laget er på plass
//TODO: returner en ReservationResponse i stedet for Reservation-entiteten direkte