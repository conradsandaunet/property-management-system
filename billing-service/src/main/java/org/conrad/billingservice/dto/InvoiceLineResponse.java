package org.conrad.billingservice.dto;

import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        InvoiceCategory category,
        String description,
        BigDecimal amount
) {
    public static InvoiceLineResponse from(InvoiceLine line) {
        return new InvoiceLineResponse(
                line.getCategory(),
                line.getDescription(),
                line.getAmount());
    }
}
