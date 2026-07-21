package com.ledgerly.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module contract for the {@code invoice} module. Other modules depend on
 * this interface so Spring Modulith can verify the dependency direction.
 */
public interface InvoiceAPI {

    Optional<Invoice> findById(UUID id);

    List<Invoice> findByCustomerId(UUID customerId);

    List<Invoice> findOverdueInvoices();

    /**
     * Marks the invoice as PAID and publishes {@link InvoicePaidEvent}.
     *
     * <p>Invoked by the {@code payment} module after a successful gateway
     * charge. The implementation validates that the invoice is in a payable
     * state and that the paid amount matches the invoice total.
     */
    Invoice markAsPaid(UUID invoiceId, BigDecimal amountPaid);
}
