package org.conrad.reservationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_seq")
    @SequenceGenerator(name = "resource_seq", sequenceName = "resource_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceStatus status;

    @Column(name = "maintenance_from")
    private LocalDateTime maintenanceFrom;

    @Column(name = "maintenance_to")
    private LocalDateTime maintenanceTo;

    /**
     * True if the resource can't be booked in [start, end): either it's set to MAINTENANCE
     * (blocked until the board sets it back), or the period overlaps the scheduled block.
     */
    public boolean isBlockedDuring(LocalDateTime start, LocalDateTime end) {
        if (status == ResourceStatus.MAINTENANCE) {
            return true;
        }
        return maintenanceFrom != null && maintenanceTo != null
                && maintenanceFrom.isBefore(end) && maintenanceTo.isAfter(start);
    }

}