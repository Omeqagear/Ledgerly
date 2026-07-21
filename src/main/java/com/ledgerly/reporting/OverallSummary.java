package com.ledgerly.reporting;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregated totals across the entire ledger.
 *
 * @param totalCustomers     number of customers tracked
 * @param invoicesByStatus   invoice counts grouped by status
 * @param paymentsByStatus   payment counts grouped by status
 * @param totalAmountPaid    sum of all completed payment amounts
 * @param totalOutstanding   sum of totalAmount for ISSUED+OVERDUE invoices
 */
public record OverallSummary(
    long totalCustomers,
    Map<String, Long> invoicesByStatus,
    Map<String, Long> paymentsByStatus,
    BigDecimal totalAmountPaid,
    BigDecimal totalOutstanding
) {}