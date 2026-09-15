package org.conrad.billingservice.service;

import lombok.RequiredArgsConstructor;
import org.conrad.billingservice.dto.InvoiceResponse;
import org.conrad.billingservice.exception.InvoiceAlreadyPaidException;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceStatus;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Override
    public InvoiceResponse payInvoice(Long id, Long residentId) {
        Invoice invoice = invoiceRepository.findByIdAndResidentId(id, residentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found for this resident"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvoiceAlreadyPaidException(id);
        }

        invoice.setStatus(InvoiceStatus.PAID);
        Invoice saved = invoiceRepository.save(invoice);
        return InvoiceResponse.from(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id, Long residentId) {
        Invoice invoice = invoiceRepository.findByIdAndResidentId(id, residentId).
                orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found for this resident"));

        return InvoiceResponse.from(invoice);
    }
}
