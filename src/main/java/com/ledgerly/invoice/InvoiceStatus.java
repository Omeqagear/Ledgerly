package com.ledgerly.invoice;

/**
 * Lifecycle states of an {@link Invoice}.
 *
 * <pre>
 * DRAFT ──issue()──▶ ISSUED ──markAsPaid()──▶ PAID
 *                       │
 *                       └──▶ OVERDUE (when dueDate passes while ISSUED)
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    OVERDUE,
    CANCELLED
}