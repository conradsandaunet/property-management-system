package org.conrad.billingservice.dto;

import java.util.List;

public record PaymentOverviewResponse(
        String period,
        long paid,
        long pending,
        long overdue,
        List<InvoiceResponse> invoices
) {
}