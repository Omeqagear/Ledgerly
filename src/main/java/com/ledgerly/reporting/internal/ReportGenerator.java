package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.CustomerSummary;
import com.ledgerly.reporting.OverallSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Composes the reporting module's rollout DTOs by delegating to
 * {@link ReportRepository}. Pure read-side orchestration; no application
 * services from other modules are injected here.
 */
@Component
public class ReportGenerator {

    private final ReportRepository reportRepository;

    ReportGenerator(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public OverallSummary overall() {
        return overall(null, null);
    }

    public OverallSummary overall(LocalDate from, LocalDate to) {
        long totalCustomers = reportRepository.countCustomers();

        Map<String, Long> invoicesByStatus = toStatusMap(reportRepository.invoiceCountsByStatus(from, to));
        Map<String, Long> paymentsByStatus = toStatusMap(reportRepository.paymentCountsByStatus());

        BigDecimal totalAmountPaid = reportRepository.totalAmountPaid();
        BigDecimal totalOutstanding = reportRepository.totalOutstanding();

        return new OverallSummary(totalCustomers, invoicesByStatus, paymentsByStatus, totalAmountPaid, totalOutstanding);
    }

    public CustomerSummary forCustomer(UUID customerId) {
        String name = reportRepository.customerName(customerId);
        if (name == null) {
            throw new NoSuchElementException("Unknown customer: " + customerId);
        }
        return new CustomerSummary(
            customerId,
            name,
            reportRepository.countInvoicesForCustomer(customerId),
            reportRepository.countPaidInvoicesForCustomer(customerId),
            reportRepository.totalInvoicedForCustomer(customerId),
            reportRepository.totalPaidForCustomer(customerId),
            reportRepository.totalOutstandingForCustomer(customerId)
        );
    }

    private static Map<String, Long> toStatusMap(java.util.List<ReportRepository.StatusCount> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ReportRepository.StatusCount c : counts) {
            result.put(c.status(), c.count());
        }
        return result;
    }
}