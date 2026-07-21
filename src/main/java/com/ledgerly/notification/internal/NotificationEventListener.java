package com.ledgerly.notification.internal;

import com.ledgerly.customer.CustomerLookup;
import com.ledgerly.invoice.InvoiceCreatedEvent;
import com.ledgerly.invoice.InvoicePaidEvent;
import com.ledgerly.payment.PaymentProcessedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for domain events published by the {@code invoice} and
 * {@code payment} modules and dispatches customer-facing emails. The customer
 * email is looked up through {@link CustomerLookup} so this module never
 * touches the customer entity directly.
 */
@Component
class NotificationEventListener {

    private final EmailSender emailSender;
    private final CustomerLookup customerLookup;
    private final TemplateEngine templateEngine;

    NotificationEventListener(EmailSender emailSender,
                              CustomerLookup customerLookup,
                              TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.customerLookup = customerLookup;
        this.templateEngine = templateEngine;
    }

    @ApplicationModuleListener
    void onInvoiceCreated(InvoiceCreatedEvent event) {
        customerLookup.findEmailById(event.customerId()).ifPresent(email ->
            emailSender.send(
                email,
                "Invoice " + event.invoiceNumber() + " created",
                templateEngine.renderInvoiceCreated(event.invoiceNumber(), event.totalAmount())
            )
        );
    }

    @ApplicationModuleListener
    void onInvoicePaid(InvoicePaidEvent event) {
        customerLookup.findEmailById(event.customerId()).ifPresent(email ->
            emailSender.send(
                email,
                "Payment received",
                templateEngine.renderInvoicePaid(event.invoiceId().toString(), event.amountPaid())
            )
        );
    }

    @ApplicationModuleListener
    void onPaymentProcessed(PaymentProcessedEvent event) {
        if (event.success()) {
            return;
        }
        customerLookup.findEmailById(event.customerId()).ifPresent(email ->
            emailSender.send(
                email,
                "Payment failed",
                templateEngine.renderPaymentFailed(
                    event.invoiceId().toString(),
                    event.failureReason() == null ? "unknown" : event.failureReason()
                )
            )
        );
    }
}