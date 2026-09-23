package org.conrad.reservationservice.repository;

import org.conrad.reservationservice.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
