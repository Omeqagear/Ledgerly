package com.ledgerly.reporting;

import java.math.BigDecimal;

/**
 * A single bucket in an accounts-receivable aging report, grouping
 * outstanding invoices by how long past their due date they are.
 *
 * @param name          bucket label (e.g. {@code "Current"}, {@code "1-30 days"})
 * @param invoiceCount  number of outstanding invoices in the bucket
 * @param totalAmount   sum of {@code totalAmount} across invoices in the bucket
 */
public record AgingBucket(String name, long invoiceCount, BigDecimal totalAmount) {}