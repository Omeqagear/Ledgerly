/**
 * Invoice module: owns the invoice aggregate and its lifecycle. Emits
 * {@link com.ledgerly.invoice.InvoiceCreatedEvent} and
 * {@link com.ledgerly.invoice.InvoicePaidEvent}. The {@code payment} module
 * depends on this module to validate and mark invoices as paid.
 *
 * <p>The invoice module references customers only by {@link java.util.UUID}, so it
 * has no compile-time dependency on the {@code customer} module.
 */
@org.springframework.modulith.ApplicationModule(
    id = "invoice",
    displayName = "Invoice Management"
)
package com.ledgerly.invoice;
