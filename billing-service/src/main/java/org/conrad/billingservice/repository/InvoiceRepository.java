package org.conrad.billingservice.repository;

import org.conrad.billingservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByApartmentIdOrderByPeriodDesc(Long apartmentId);

    Optional<Invoice> findFirstByApartmentIdOrderByPeriodDesc(Long apartmentId);

    Optional<Invoice> findByIdAndApartmentId(Long id, Long apartmentId);

    boolean existsByApartmentIdAndPeriod(Long apartmentId, String period);

    List<Invoice> findByPeriod(String period);
}