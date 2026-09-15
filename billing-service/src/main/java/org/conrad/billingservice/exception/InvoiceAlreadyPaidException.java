package org.conrad.billingservice.exception;

public class InvoiceAlreadyPaidException extends RuntimeException {
    public InvoiceAlreadyPaidException(Long id) {
        super("Invoice already paid for id: " + id);
    }
}
