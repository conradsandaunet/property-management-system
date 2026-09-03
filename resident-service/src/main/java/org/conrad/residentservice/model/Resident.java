package org.conrad.residentservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resident")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resident_seq")
    @SequenceGenerator(name = "resident_seq", sequenceName = "resident_seq", allocationSize = 1)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "is_manager", nullable = false)
    private boolean manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

}
