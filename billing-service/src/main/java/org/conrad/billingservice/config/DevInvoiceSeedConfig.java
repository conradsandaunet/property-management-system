package org.conrad.billingservice.config;

import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;
import org.conrad.billingservice.model.InvoiceStatus;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Demo invoices for apartments 1-6 (the ones resident-service seeds): last month mostly paid, one
 * overdue, and this month's still pending.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DevInvoiceSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevInvoiceSeedConfig.class);
    private static final int APARTMENTS = 6;

    private final InvoiceRepository invoiceRepository;

    public DevInvoiceSeedConfig(InvoiceRepository invoiceRepository) { this.invoiceRepository = invoiceRepository; }

    @Override
    public void run(ApplicationArguments args) {
        if (invoiceRepository.count() > 0) {
            return;
        }

        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);

        for (long apartmentId = 1; apartmentId <= APARTMENTS; apartmentId++) {
            // Apartment 4 never paid last month's invoice, so it shows up as overdue.
            InvoiceStatus lastMonthStatus = apartmentId == 4 ? InvoiceStatus.PENDING : InvoiceStatus.PAID;
            invoiceRepository.save(invoice(apartmentId, lastMonth, lastMonthStatus));
            invoiceRepository.save(invoice(apartmentId, thisMonth, InvoiceStatus.PENDING));
        }
        log.info("Seeded demo invoices for {} apartments ({} and {})", APARTMENTS, lastMonth, thisMonth);
    }

    private Invoice invoice(Long apartmentId, YearMonth period, InvoiceStatus status) {
        Invoice invoice = Invoice.builder()
                .apartmentId(apartmentId)
                .period(period.toString())
                .dueDate(period.atDay(15))
                .status(status)
                .build();

        invoice.addLine(line(InvoiceCategory.SHARED_COST, "Felleskostnader - " + period, new BigDecimal("3200.00")));
        invoice.addLine(line(InvoiceCategory.WATER, "Vann og avløp", new BigDecimal("350.00")));
        // Apartments 1-3 have a parking space.
        if (apartmentId <= 3) {
            invoice.addLine(line(InvoiceCategory.PARKING, "Parkeringsplass", new BigDecimal("500.00")));
        }
        return invoice;
    }

    private InvoiceLine line(InvoiceCategory category, String description, BigDecimal amount) {
        return InvoiceLine.builder()
                .category(category)
                .description(description)
                .amount(amount)
                .build();
    }
}
