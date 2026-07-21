package com.ledgerly.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published by the invoice module after a successful payment has been processed
 * (by the {@code payment} module) — i.e. when the invoice transitions to PAID.
 */
public record InvoicePaidEvent(
    UUID invoiceId,
    UUID customerId,
    BigDecimal amountPaid,
    LocalDateTime paidAt
) {}