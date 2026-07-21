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
    FAILED
}