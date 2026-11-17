package org.conrad.residentservice.repository;

import org.conrad.residentservice.model.ApartmentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentPhotoRepository extends JpaRepository<ApartmentPhoto, Long> {

    /** Metadata only, so listing doesn't load every image's bytes. */
    interface Summary {
        Long getId();
        String getFileName();
        String getContentType();
        int getSizeBytes();
    }

    List<Summary> findByApartmentIdOrderByIdAsc(Long apartmentId);

    long countByApartmentId(Long apartmentId);

    Optional<ApartmentPhoto> findByIdAndApartmentId(Long id, Long apartmentId);
}
