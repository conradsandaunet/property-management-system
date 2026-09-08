package org.conrad.billingservice.repository;

import org.conrad.billingservice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByResidentIdOrderByPeriodDesc(Long residentId);

    Optional<Invoice> findFirstByResidentIdOrderByPeriodDesc(Long residentId);

    Optional<Invoice> findByIdAndResidentId(Long id, Long residentId);
}
