package org.conrad.residentservice.repository;

import org.conrad.residentservice.model.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;



public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
}
