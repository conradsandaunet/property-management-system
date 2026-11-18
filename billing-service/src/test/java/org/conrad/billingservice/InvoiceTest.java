package org.conrad.billingservice;

import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;
import org.conrad.billingservice.model.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test (no Spring, no database) for the invoice's own rules: when it counts as overdue, and what the total is.
 * User story: as a property manager I want to see paid / pending / overdue, so I can follow up on outstanding payments.
 */
class InvoiceTest {

    private static Invoice invoice(InvoiceStatus status, LocalDate dueDate) {
        return Invoice.builder().apartmentId(1L).period("2030-01").status(status).dueDate(dueDate).build();
    }

    @Test
    @DisplayName("An unpaid invoice becomes overdue the day after the due date, not on the due date")
    void unpaidInvoiceIsOverdueOnlyAfterTheDueDate() {
        LocalDate today = LocalDate.now();

        assertThat(invoice(InvoiceStatus.PENDING, today.minusDays(1)).getEffectiveStatus())
                .isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(invoice(InvoiceStatus.PENDING, today).getEffectiveStatus())
                .isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    @DisplayName("A paid invoice is never shown as overdue, even long after the due date")
    void paidInvoiceIsNeverOverdue() {
        assertThat(invoice(InvoiceStatus.PAID, LocalDate.now().minusYears(1)).getEffectiveStatus())
                .isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("The total is the exact sum of the lines, with no rounding errors")
    void totalIsTheExactSumOfTheLines() {
        Invoice invoice = invoice(InvoiceStatus.PENDING, LocalDate.now());
        invoice.addLine(line(InvoiceCategory.SHARED_COST, "3200.00"));
        invoice.addLine(line(InvoiceCategory.WATER, "350.10"));
        invoice.addLine(line(InvoiceCategory.PARKING, "499.90"));

        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("4050.00");
    }

    private static InvoiceLine line(InvoiceCategory category, String amount) {
        return InvoiceLine.builder().category(category).description(category.name()).amount(new BigDecimal(amount)).build();
    }
}
