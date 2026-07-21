/**
 * Payment module: owns the payment aggregate and the gateway integration. It
 * depends on the {@code invoice} module (via {@link com.ledgerly.invoice.InvoiceAPI})
 * to validate invoice state and amount and to mark invoices as paid after a
 * successful charge. After every payment it publishes
 * {@link com.ledgerly.payment.PaymentProcessedEvent}; the
 * {@code notification} module listens to failure events.
 */
@org.springframework.modulith.ApplicationModule(
    id = "payment",
    displayName = "Payment Processing",
    allowedDependencies = {"invoice"}
)
package com.ledgerly.payment;
