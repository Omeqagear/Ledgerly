package com.ledgerly.invoice.internal;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceService;
import com.ledgerly.payment.PaymentProcessedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Reacts to {@link PaymentProcessedEvent}s emitted by the {@code payment}
 * module. On success the invoice is marked as PAID and an
 * {@link com.ledgerly.invoice.InvoicePaidEvent} is published for the
 * notification module.
 *
 * <p>This listener is internal to the invoice module — and so is the
 * {@link InvoiceService#markAsPaid(UUID, java.math.BigDecimal)} call it makes.
 *
 * <p>Note: Modulith's {@code @ApplicationModuleListener} already runs the
 * method in a transaction (REQUIRES_NEW), so a plain {@code @Transactional}
 * is intentionally NOT declared here — Spring would reject the combination.
 */
@Component
class PaymentEventListener {

    private final InvoiceService invoiceService;

    PaymentEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @ApplicationModuleListener
    void onPaymentProcessed(PaymentProcessedEvent event) {
        if (!event.success()) {
            return;
        }
        invoiceService.markAsPaid(event.invoiceId(), event.amount());
    }
}