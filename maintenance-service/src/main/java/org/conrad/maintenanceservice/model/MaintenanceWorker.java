package org.conrad.maintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "maintenance_worker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "maintenance_worker_seq")
    @SequenceGenerator(name = "maintenance_worker_seq", sequenceName = "maintenance_worker_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "maintenance_worker_specialty", joinColumns = @JoinColumn(name =
    "worker_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Set<MaintenanceCategory> specialties = new HashSet<>();
}
