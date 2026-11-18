package org.conrad.billingservice.service;

import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** The standard monthly charges and due date. Used when generating invoices and for the demo data. */
public final class InvoiceDefaults {

    private static final int DUE_DAY = 15;
    private static final BigDecimal SHARED_COST = new BigDecimal("3200.00");
    private static final BigDecimal WATER = new BigDecimal("350.00");
    private static final BigDecimal PARKING = new BigDecimal("500.00");

    private InvoiceDefaults() {}

    public static LocalDate dueDate(YearMonth period) {
        return period.atDay(DUE_DAY);
    }

    public static InvoiceLine sharedCost(YearMonth period) {
        return line(InvoiceCategory.SHARED_COST, "Felleskostnader - " + period, SHARED_COST);
    }

    public static InvoiceLine water() {
        return line(InvoiceCategory.WATER, "Vann og avløp", WATER);
    }

    public static InvoiceLine parking() {
        return line(InvoiceCategory.PARKING, "Parkeringsplass", PARKING);
    }

    private static InvoiceLine line(InvoiceCategory category, String description, BigDecimal amount) {
        return InvoiceLine.builder().category(category).description(description).amount(amount).build();
    }
}
