package com.ledgerly.reporting;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated totals across the entire ledger.
 *
 * <p>When a date range is applied via {@code from}/{@code to} parameters, only the
 * {@code invoicesByStatus} breakdown is filtered by invoice issue date. The
 * remaining metrics ({@code totalCustomers}, {@code paymentsByStatus},
 * {@code totalAmountPaid}, {@code totalOutstanding}) always reflect all-time totals.
 *
 * @param totalCustomers     number of customers tracked
 * @param invoicesByStatus   invoice counts grouped by status (filtered by date range when applicable)
 * @param paymentsByStatus   payment counts grouped by status (all-time)
 * @param totalAmountPaid    sum of all completed payment amounts (all-time)
 * @param totalOutstanding   sum of totalAmount for ISSUED+OVERDUE invoices (all-time)
 */
public record OverallSummary(
    long totalCustomers,
    Map<String, Long> invoicesByStatus,
    Map<String, Long> paymentsByStatus,
    BigDecimal totalAmountPaid,
    BigDecimal totalOutstanding
) {}