package org.conrad.residentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "apartment_photo")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApartmentPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_photo_seq")
    @SequenceGenerator(name = "apartment_photo_seq", sequenceName = "apartment_photo_seq", allocationSize = 1)
    private Long id;

    @Column(name = "apartment_id", nullable = false)
    private Long apartmentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
