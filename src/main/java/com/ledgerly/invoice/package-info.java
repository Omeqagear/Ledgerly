/**
 * Invoice module: owns the invoice aggregate and its lifecycle. Consumes
 * {@code com.ledgerly.payment.PaymentProcessedEvent} (declaring a dependency on
 * the {@code payment} module) and emits {@link com.ledgerly.invoice.InvoiceCreatedEvent}
 * and {@link com.ledgerly.invoice.InvoicePaidEvent}.
 *
 * <p>The invoice module references customers only by {@link java.util.UUID}, so it
 * has no compile-time dependency on the {@code customer} module.
 */
@org.springframework.modulith.ApplicationModule(
    id = "invoice",
    displayName = "Invoice Management",
    allowedDependencies = {"payment"}
)
package com.ledgerly.invoice;