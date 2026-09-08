package org.conrad.billingservice.dto;

import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String period,
        LocalDate dueDate,
        InvoiceStatus status,
        BigDecimal totalAmount,
        List<InvoiceLineResponse> lines
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getPeriod(),
                invoice.getDueDate(),
                invoice.getStatus(),
                invoice.getTotalAmount(),
                invoice.getLines().stream().map(InvoiceLineResponse::from).toList()
        );
    }
}
