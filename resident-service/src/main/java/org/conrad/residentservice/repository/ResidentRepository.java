package org.conrad.residentservice.repository;

import org.conrad.residentservice.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    Optional<Resident> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Resident> findByApartmentIdAndActiveTrue(Long apartmentId);

    long countByActiveTrue();

    @Query("select count(distinct r.apartment.id) from Resident r where r.active = true")
    long countOccupiedApartments();

    @Query("""
            select r from Resident r join r.apartment a
            where (:includeInactive = true or r.active = true)
              and (lower(r.firstName) like lower(concat('%', :query, '%'))
                or lower(r.lastName) like lower(concat('%', :query, '%'))
                or lower(concat(r.firstName, ' ', r.lastName)) like lower(concat('%', :query, '%'))
                or lower(a.apartmentNumber) like lower(concat('%', :query, '%')))
            order by r.lastName, r.firstName
            """)
    List<Resident> search(@Param("query") String query, @Param("includeInactive") boolean includeInactive);
}
