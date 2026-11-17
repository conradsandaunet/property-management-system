package org.conrad.residentservice.dto;

import org.conrad.residentservice.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long requestId,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getRequestId(), n.getMessage(),
                n.isRead(), n.getCreatedAt());
    }
}
