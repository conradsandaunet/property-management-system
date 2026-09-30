package org.conrad.reservationservice.repository;

import org.conrad.reservationservice.model.Reservation;
import org.conrad.reservationservice.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByResourceIdAndStatus(Long resourceId, ReservationStatus status);

    List<Reservation> findByResidentId(Long residentId);

    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.resource.id = :resourceId
            AND r.status = 'ACTIVE'
            AND r.startTime < :endTime
            AND r.endTime > :startTime
            """)
    boolean existsOverlapping(
            @Param("resourceId") Long resourceId,
            @Param("startTime")LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
            );
}
