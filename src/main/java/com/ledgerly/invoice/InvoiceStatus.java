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
    CANCELLED;

    public static final String DRAFT_STR = "DRAFT";
    public static final String ISSUED_STR = "ISSUED";
    public static final String PAID_STR = "PAID";
    public static final String OVERDUE_STR = "OVERDUE";
    public static final String CANCELLED_STR = "CANCELLED";
}