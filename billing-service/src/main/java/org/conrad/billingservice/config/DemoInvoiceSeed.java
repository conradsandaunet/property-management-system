package org.conrad.billingservice.config;

import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceStatus;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.conrad.billingservice.service.InvoiceDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.conrad.security.SeedLock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Demo invoices for apartments 1-6 (the ones resident-service seeds): last month mostly paid, one
 * overdue, and this month's still pending.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoInvoiceSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoInvoiceSeed.class);
    private static final int APARTMENTS = 6;

    private final SeedLock seedLock;
    private final InvoiceRepository invoiceRepository;

    public DemoInvoiceSeed(SeedLock seedLock, InvoiceRepository invoiceRepository) {
        this.seedLock = seedLock;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Several instances may start together; one seeds, the others wait and then find the data.
        seedLock.runExclusively(this::seed);
    }

    private void seed() {
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
                .dueDate(InvoiceDefaults.dueDate(period))
                .status(status)
                .build();

        invoice.addLine(InvoiceDefaults.sharedCost(period));
        invoice.addLine(InvoiceDefaults.water());
        // Apartments 1-3 have a parking space.
        if (apartmentId <= 3) {
            invoice.addLine(InvoiceDefaults.parking());
        }
        return invoice;
    }

}
