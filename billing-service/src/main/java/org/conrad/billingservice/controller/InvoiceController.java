package org.conrad.billingservice.controller;

import org.conrad.billingservice.dto.PaymentOverviewResponse;
import org.conrad.billingservice.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.conrad.billingservice.dto.InvoiceResponse;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.conrad.billingservice.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceRepository invoiceRepository, InvoiceService invoiceService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
    }

    // --- Resident: always their own apartment ---

    @GetMapping("/current")
    public InvoiceResponse current(Authentication authentication) {
        Long apartmentId = CurrentUser.apartmentId(authentication);
        Invoice invoice = invoiceRepository.findFirstByApartmentIdOrderByPeriodDesc(apartmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No invoice found for this apartment"));
        return InvoiceResponse.from(invoice);
    }

    @GetMapping
    public List<InvoiceResponse> history(Authentication authentication) {
        Long apartmentId = CurrentUser.apartmentId(authentication);
        return invoiceRepository.findByApartmentIdOrderByPeriodDesc(apartmentId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public InvoiceResponse one(@PathVariable Long id, Authentication authentication) {
        return invoiceService.getInvoiceById(id, CurrentUser.apartmentId(authentication));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable Long id, Authentication authentication) {
        Long apartmentId = CurrentUser.apartmentId(authentication);
        return ResponseEntity.ok(invoiceService.payInvoice(id, apartmentId));
    }

    // --- Board: whole building. period is "YYYY-MM", defaults to the current month ---

    @PostMapping("/generate")
    @PreAuthorize("hasRole('BOARD')")
    public List<InvoiceResponse> generate(@RequestParam(required = false) YearMonth period) {
        return invoiceService.generateMonthlyInvoices(period != null ? period : YearMonth.now());
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('BOARD')")
    public PaymentOverviewResponse overview(@RequestParam(required = false) YearMonth period) {
        return invoiceService.getPaymentOverview(period != null ? period : YearMonth.now());
    }

    @PatchMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('BOARD')")
    public InvoiceResponse markPaid(@PathVariable Long id) {
        return invoiceService.markInvoicePaid(id);
    }

}