package com.ledgerly.reporting;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Per-customer roll-up used by the reporting module.
 *
 * @param customerId         customer id
 * @param customerName       customer display name
 * @param invoiceCount       number of invoices for the customer
 * @param paidInvoiceCount   number of invoices in PAID status
 * @param totalInvoiced      sum of totalAmount across the customer's invoices
 * @param totalPaid          sum of completed payments for the customer
 * @param totalOutstanding   sum of totalAmount for ISSUED+OVERDUE invoices
 */
public record CustomerSummary(
    UUID customerId,
    String customerName,
    long invoiceCount,
    long paidInvoiceCount,
    BigDecimal totalInvoiced,
    BigDecimal totalPaid,
    BigDecimal totalOutstanding
) {}