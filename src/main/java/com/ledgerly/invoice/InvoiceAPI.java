package com.ledgerly.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only contract for other modules that need to reference invoices. Write
 * operations remain on {@link InvoiceService} and are only to be invoked from
 * within the invoice module or through the REST API.
 */
public interface InvoiceAPI {

    Optional<Invoice> findById(UUID id);

    List<Invoice> findByCustomerId(UUID customerId);

    List<Invoice> findOverdueInvoices();
}