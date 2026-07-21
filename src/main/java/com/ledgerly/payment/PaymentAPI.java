package com.ledgerly.payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-side contract for other modules that need to inspect payment records
 * (e.g. reporting).
 */
public interface PaymentAPI {

    Optional<Payment> findById(UUID id);

    java.util.List<Payment> findByInvoiceId(UUID invoiceId);

    java.util.List<Payment> findByCustomerId(UUID customerId);
}