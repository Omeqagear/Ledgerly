package com.ledgerly.payment;

/**
 * Status of a {@link Payment}.
 *
 * <pre>
 * PENDING ──process──▶ COMPLETED  (gateway returned success)
 * PENDING ──process──▶ FAILED     (gateway returned failure)
 * </pre>
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED;

    public static final String PENDING_STR = "PENDING";
    public static final String COMPLETED_STR = "COMPLETED";
    public static final String FAILED_STR = "FAILED";
}