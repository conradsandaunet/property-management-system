package org.conrad.residentservice.messaging;

import java.time.Instant;

public record MaintenanceCompleted(Long requestId, Long residentId, Long workerId,
                                   Instant completedAt) {
}
