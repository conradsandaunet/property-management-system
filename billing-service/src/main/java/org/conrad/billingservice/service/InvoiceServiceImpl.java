package org.conrad.billingservice.service;

import lombok.RequiredArgsConstructor;
import org.conrad.billingservice.client.ResidentServiceClient;
import org.conrad.billingservice.dto.ApartmentDto;
import org.conrad.billingservice.dto.InvoiceResponse;
import org.conrad.billingservice.dto.PaymentOverviewResponse;
import org.conrad.billingservice.exception.InvoiceAlreadyPaidException;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;
import org.conrad.billingservice.model.InvoiceStatus;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ResidentServiceClient residentServiceClient;

    @Override
    public InvoiceResponse payInvoice(Long id, Long apartmentId) {
        Invoice invoice = invoiceRepository.findByIdAndApartmentId(id, apartmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found for this apartment"));

        return InvoiceResponse.from(markPaid(invoice));
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id, Long apartmentId) {
        Invoice invoice = invoiceRepository.findByIdAndApartmentId(id, apartmentId).
                orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice not found for this apartment"));

        return InvoiceResponse.from(invoice);
    }

    @Override
    public List<InvoiceResponse> generateMonthlyInvoices(YearMonth period) {
        List<InvoiceResponse> created = new ArrayList<>();

        for (ApartmentDto apartment : residentServiceClient.getAllApartments()) {
            // Skip apartments that already have an invoice for this month, so it's safe to run twice
            if (invoiceRepository.existsByApartmentIdAndPeriod(apartment.id(), period.toString())) {
                continue;
            }

            Invoice invoice = Invoice.builder()
                    .apartmentId(apartment.id())
                    .period(period.toString())
                    .dueDate(period.atDay(15))
                    .build();
            invoice.addLine(line(InvoiceCategory.SHARED_COST, "Felleskostnader - " + period, "3200.00"));
            invoice.addLine(line(InvoiceCategory.WATER, "Vann og avløp", "350.00"));

            created.add(InvoiceResponse.from(invoiceRepository.save(invoice)));
        }
        return created;
    }

    @Override
    public PaymentOverviewResponse getPaymentOverview(YearMonth period) {
        List<InvoiceResponse> invoices = invoiceRepository.findByPeriod(period.toString()).stream()
                .map(InvoiceResponse::from)
                .toList();

        // InvoiceResponse.status is the effective status, so OVERDUE is counted correctly
        return new PaymentOverviewResponse(
                period.toString(),
                countWithStatus(invoices, InvoiceStatus.PAID),
                countWithStatus(invoices, InvoiceStatus.PENDING),
                countWithStatus(invoices, InvoiceStatus.OVERDUE),
                invoices
        );
    }

    @Override
    public InvoiceResponse markInvoicePaid(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        return InvoiceResponse.from(markPaid(invoice));
    }

    private Invoice markPaid(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvoiceAlreadyPaidException(invoice.getId());
        }
        invoice.setStatus(InvoiceStatus.PAID);
        return invoiceRepository.save(invoice);
    }

    private long countWithStatus(List<InvoiceResponse> invoices, InvoiceStatus status) {
        return invoices.stream().filter(i -> i.status() == status).count();
    }

    private InvoiceLine line(InvoiceCategory category, String description, String amount) {
        return InvoiceLine.builder()
                .category(category)
                .description(description)
                .amount(new BigDecimal(amount))
                .build();
    }
}