package com.ledgerly.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published when an invoice is first created (status = DRAFT).
 */
public record InvoiceCreatedEvent(
    UUID invoiceId,
    UUID customerId,
    String invoiceNumber,
    BigDecimal totalAmount,
    LocalDateTime createdAt
) {
    public static InvoiceCreatedEvent from(Invoice invoice) {
        return new InvoiceCreatedEvent(
            invoice.getId(),
            invoice.getCustomerId(),
            invoice.getInvoiceNumber(),
            invoice.getTotalAmount(),
            LocalDateTime.now()
        );
    }
}