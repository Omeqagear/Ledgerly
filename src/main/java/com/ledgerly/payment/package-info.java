/**
 * Payment module: owns the payment aggregate and the gateway integration. The
 * module depends on no other application module — invoice / customer
 * identifiers are passed in by the caller and persisted id-only. After every
 * payment it publishes {@link com.ledgerly.payment.PaymentProcessedEvent}; the
 * {@code invoice} module listens to that event to mark invoices as PAID.
 */
@org.springframework.modulith.ApplicationModule(
    id = "payment",
    displayName = "Payment Processing"
)
package com.ledgerly.payment;