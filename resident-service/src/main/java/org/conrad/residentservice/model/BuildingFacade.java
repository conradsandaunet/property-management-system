package org.conrad.residentservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "building_facade")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BuildingFacade {

    @Id
    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(nullable = false)
    private byte[] data;
}
