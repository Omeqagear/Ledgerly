package com.ledgerly.invoice.internal;

import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates sequential, year-stamped invoice numbers of the form
 * {@code INV-2024-000001}.
 *
 * <p>The counter is in-memory only; uniqueness across restarts is enforced by
 * the unique constraint on {@code invoices.invoice_number} and is sufficient
 * for the scaffold. A production implementation would either seed from the DB
 * or use a sequence table.
 */
@Component
public class InvoiceNumberGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);

    public String generate() {
        return "INV-" + Year.now() + "-" + String.format("%06d", counter.incrementAndGet());
    }
}