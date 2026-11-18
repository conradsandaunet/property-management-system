package org.conrad.maintenanceservice.dto;

import org.conrad.maintenanceservice.model.CommentAuthorType;
import org.conrad.maintenanceservice.model.MaintenanceComment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        CommentAuthorType authorType,
        String authorName,
        String text,
        LocalDateTime createdAt
) {
    public static CommentResponse from(MaintenanceComment c) {
        return new CommentResponse(c.getId(), c.getAuthorType(), c.getAuthorName(),
                c.getText(), c.getCreatedAt());
    }
}
