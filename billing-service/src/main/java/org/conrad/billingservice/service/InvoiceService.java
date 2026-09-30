package org.conrad.billingservice.service;

import org.conrad.billingservice.dto.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse getInvoiceById(Long id, Long residentId);
    InvoiceResponse payInvoice(Long id, Long residentId);

}
