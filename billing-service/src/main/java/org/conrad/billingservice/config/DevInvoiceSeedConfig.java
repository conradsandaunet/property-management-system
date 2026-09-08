package org.conrad.billingservice.config;

import org.conrad.billingservice.dto.InvoiceLineResponse;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class DevInvoiceSeedConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevInvoiceSeedConfig.class);
    private static final Long SEED_RESIDENT_ID = 1L;

    private final InvoiceRepository invoiceRepository;

    public DevInvoiceSeedConfig(InvoiceRepository invoiceRepository) { this.invoiceRepository = invoiceRepository; }

    @Override
    public void run(ApplicationArguments args) {
        if (invoiceRepository.count() > 0) {
            return;
        }

        YearMonth period = YearMonth.now();
        LocalDate dueDate = period.atDay(15);

        Invoice invoice = Invoice.builder()
                .residentId(SEED_RESIDENT_ID)
                .period(period.toString())
                .dueDate(dueDate)
                .build();

        invoice.addLine(line(InvoiceCategory.SHARED_COST, "Felleskostnader - " + period, new BigDecimal("3200.00")));
        invoice.addLine(line(InvoiceCategory.WATER, "Vann og avløp", new BigDecimal("350.00")));
        invoice.addLine(line(InvoiceCategory.PARKING, "Parkeringsplass", new BigDecimal("500.00")));

        invoiceRepository.save(invoice);
        log.info("Seeded a demo invoice for residentId={} (period={})", SEED_RESIDENT_ID, period);
    }

    private InvoiceLine line(InvoiceCategory category, String description, BigDecimal amount) {
        return InvoiceLine.builder()
                .category(category)
                .description(description)
                .amount(amount)
                .build();
    }
}
