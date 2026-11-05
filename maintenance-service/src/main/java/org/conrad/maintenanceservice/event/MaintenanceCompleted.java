package org.conrad.maintenanceservice.event;

import java.time.Instant;

public record MaintenanceCompleted(Long requestId, Long residentId, Long workerId,
                                   Instant completedAt) {
}
