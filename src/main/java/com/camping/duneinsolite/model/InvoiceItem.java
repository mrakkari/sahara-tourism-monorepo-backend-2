package com.camping.duneinsolite.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "invoice_item_id", updatable = false, nullable = false)
    private UUID invoiceItemId;

    // Many items belong to one invoice
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "description", nullable = false)
    private String description;

    // e.g. "TOUR", "EXTRA", "FEE", "DISCOUNT"
    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    // Line number controls display order on the invoice
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    // ──────────────────────────────────────────────
    // COMPUTED FIELD — not stored in DB
    // totalPrice = quantity * unitPrice
    // ──────────────────────────────────────────────
    @Transient
    public Double getTotalPrice() {
        if (quantity == null || unitPrice == null) return 0.0;
        return quantity * unitPrice;
    }
}
