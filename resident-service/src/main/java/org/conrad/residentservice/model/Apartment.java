package org.conrad.residentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apartment")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_seq")
    @SequenceGenerator(name = "apartment_seq", sequenceName = "apartment_seq", allocationSize = 1)
    private Long id;

    @Column(name = "apartment_number", nullable = false, length = 10)
    private String apartmentNumber;

    @Column(nullable = false)
    private Integer floor;

    @Column(name = "building_name", nullable = false)
    private String buildingName;


    @OneToMany(mappedBy = "apartment")
    @Builder.Default
    private List<Resident> residents = new ArrayList<>();

}
