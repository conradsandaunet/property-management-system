package org.conrad.residentservice.repository;

import org.conrad.residentservice.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Building> findByNameIgnoreCase(String name);
}
