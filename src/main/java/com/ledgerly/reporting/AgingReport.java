package com.ledgerly.reporting;

import java.math.BigDecimal;
import java.util.List;

/**
 * Accounts-receivable aging report: outstanding invoices grouped into
 * {@link AgingBucket buckets} by days past due, plus the overall outstanding
 * total.
 *
 * @param buckets           ordered buckets from {@code Current} to {@code 90+ days}
 * @param totalOutstanding  sum of {@code totalAmount} across all outstanding invoices
 */
public record AgingReport(List<AgingBucket> buckets, BigDecimal totalOutstanding) {}