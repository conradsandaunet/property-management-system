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

    /** The housing cooperative's bank account (demo value), shown on invoices for bank payment. */
    public static final String ACCOUNT_NUMBER = "1503.27.48261";
    private static final int KID_LENGTH = 7;

    private InvoiceDefaults() {}

    /**
     * Customer reference (KID) the resident types in the online bank: the invoice id, zero-padded,
     * plus a MOD10 (Luhn) check digit, the most common KID algorithm in Norway.
     */
    public static String kid(long invoiceId) {
        String base = String.format("%0" + KID_LENGTH + "d", invoiceId);
        int sum = 0;
        boolean doubleIt = true; // from the right, starting next to the check digit
        for (int i = base.length() - 1; i >= 0; i--) {
            int digit = base.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return base + (10 - sum % 10) % 10;
    }

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
