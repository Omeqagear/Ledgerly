package com.ledgerly.reporting.internal;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Categorizes outstanding invoices into aging buckets based on how many days
 * they are past their due date. Buckets, in order:
 * <ul>
 *   <li>{@code Current} — not yet past due (0 or negative days overdue)</li>
 *   <li>{@code 1-30 days}</li>
 *   <li>{@code 31-60 days}</li>
 *   <li>{@code 61-90 days}</li>
 *   <li>{@code 90+ days}</li>
 * </ul>
 */
@Component
public class AgingCalculator {

    public String categorize(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
        if (daysOverdue <= 0) return "Current";
        else if (daysOverdue <= 30) return "1-30 days";
        else if (daysOverdue <= 60) return "31-60 days";
        else if (daysOverdue <= 90) return "61-90 days";
        else return "90+ days";
    }

    public Map<String, List<ReportRepository.OutstandingInvoice>> groupByBucket(
            List<ReportRepository.OutstandingInvoice> invoices) {
        Map<String, List<ReportRepository.OutstandingInvoice>> grouped = new HashMap<>();
        for (ReportRepository.OutstandingInvoice invoice : invoices) {
            String bucket = categorize(invoice.dueDate());
            grouped.computeIfAbsent(bucket, k -> new ArrayList<>()).add(invoice);
        }
        return grouped;
    }
}