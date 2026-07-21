package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.CustomerSummary;
import com.ledgerly.reporting.OverallSummary;
import org.springframework.stereotype.Component;

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
        Map<String, Long> invoicesByStatus = toStatusMap(reportRepository.invoiceCountsByStatus());
        Map<String, Long> paymentsByStatus = toStatusMap(reportRepository.paymentCountsByStatus());
        return new OverallSummary(
            reportRepository.countCustomers(),
            invoicesByStatus,
            paymentsByStatus,
            reportRepository.totalAmountPaid(),
            reportRepository.totalOutstanding()
        );
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