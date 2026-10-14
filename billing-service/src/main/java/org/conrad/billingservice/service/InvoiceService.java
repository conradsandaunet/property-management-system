package org.conrad.billingservice.service;

import org.conrad.billingservice.dto.InvoiceResponse;
import org.conrad.billingservice.dto.PaymentOverviewResponse;

import java.time.YearMonth;
import java.util.List;

public interface InvoiceService {

    InvoiceResponse getInvoiceById(Long id, Long apartmentId);
    InvoiceResponse payInvoice(Long id, Long apartmentId);

    List<InvoiceResponse> generateMonthlyInvoices(YearMonth period);
    PaymentOverviewResponse getPaymentOverview(YearMonth period);
    InvoiceResponse markInvoicePaid(Long id);

}