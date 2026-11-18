package org.conrad.residentservice.repository;

import org.conrad.residentservice.model.BuildingFacade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface BuildingFacadeRepository extends JpaRepository<BuildingFacade, Long> {

    /** Which buildings have a facade, without loading the images. */
    @Query("select f.buildingId from BuildingFacade f")
    Set<Long> findBuildingIds();
}
