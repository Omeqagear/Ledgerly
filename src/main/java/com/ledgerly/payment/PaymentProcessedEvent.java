package com.ledgerly.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published by the payment module after the gateway responds. Downstream
 * modules (invoice, notification) react to this single event.
 */
public record PaymentProcessedEvent(
    UUID paymentId,
    UUID invoiceId,
    UUID customerId,
    BigDecimal amount,
    boolean success,
    String failureReason,
    LocalDateTime processedAt
) {
    public static PaymentProcessedEvent success(Payment payment) {
        return new PaymentProcessedEvent(
            payment.getId(), payment.getInvoiceId(), payment.getCustomerId(),
            payment.getAmount(), true, null, payment.getProcessedAt());
    }

    public static PaymentProcessedEvent failure(Payment payment, String reason) {
        return new PaymentProcessedEvent(
            payment.getId(), payment.getInvoiceId(), payment.getCustomerId(),
            payment.getAmount(), false, reason, payment.getProcessedAt());
    }
}