package org.conrad.billingservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_seq")
    @SequenceGenerator(name = "invoice_seq", sequenceName = "invoice_seq", allocationSize = 1)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id asc")
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    public addLine(InvoiceLine line) {
        line.setInvoice(this);
        lines.add(line);
    }

    public BigDecimal getTotalAmount() {
        return lines.stream()
                .map(InvoiceLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
