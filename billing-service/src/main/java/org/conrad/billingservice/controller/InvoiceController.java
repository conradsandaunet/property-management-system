package org.conrad.billingservice.controller;

import org.conrad.billingservice.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.conrad.billingservice.dto.InvoiceResponse;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/current")
    public InvoiceResponse current(Authentication authentication) {
        Long residentId = residentId(authentication);
        Invoice invoice = invoiceRepository.findFirstByResidentIdOrderByPeriodDesc(residentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No invoice found for this resident"));
        return InvoiceResponse.from(invoice);
    }

    @GetMapping
    public List<InvoiceResponse> history(Authentication authentication) {
        Long residentId = residentId(authentication);
        return invoiceRepository.findByResidentIdOrderByPeriodDesc(residentId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public InvoiceResponse one(@PathVariable Long id, Authentication authentication) {
        return invoiceService.getInvoiceById(id, residentId(authentication));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable Long id, Authentication authentication) {
        Long residentId = residentId(authentication);
        return ResponseEntity.ok(invoiceService.payInvoice(id, residentId));
    }


    private Long residentId(Authentication authentication) {return Long.parseLong(authentication.getName()); }
}
