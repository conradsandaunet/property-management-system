package org.conrad.reservationservice.web;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public record ReserveRequest(
            Long resourceId,
            Long residentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }
}
