package org.conrad.maintenanceservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maintenance_comment_seq")
    @SequenceGenerator(name = "maintenance_comment_seq", sequenceName = "maintenance_comment_seq", allocationSize = 1)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false)
    private CommentAuthorType authorType;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
