package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.AgingBucket;
import com.ledgerly.reporting.AgingReport;
import com.ledgerly.reporting.CustomerSummary;
import com.ledgerly.reporting.OverallSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final AgingCalculator agingCalculator;

    ReportGenerator(ReportRepository reportRepository, AgingCalculator agingCalculator) {
        this.reportRepository = reportRepository;
        this.agingCalculator = agingCalculator;
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

    public AgingReport agingReport() {
        return buildAgingReport(reportRepository.findOutstandingInvoices());
    }

    public AgingReport agingReportForCustomer(UUID customerId) {
        return buildAgingReport(reportRepository.findOutstandingInvoicesForCustomer(customerId));
    }

    private AgingReport buildAgingReport(List<ReportRepository.OutstandingInvoice> outstanding) {
        Map<String, List<ReportRepository.OutstandingInvoice>> grouped = agingCalculator.groupByBucket(outstanding);
        List<AgingBucket> buckets = createBuckets(grouped);
        BigDecimal totalOutstanding = calculateTotalOutstanding(outstanding);
        return new AgingReport(buckets, totalOutstanding);
    }

    private static List<AgingBucket> createBuckets(
            Map<String, List<ReportRepository.OutstandingInvoice>> grouped) {
        List<String> bucketOrder = List.of("Current", "1-30 days", "31-60 days", "61-90 days", "90+ days");
        return bucketOrder.stream()
            .map(bucketName -> {
                List<ReportRepository.OutstandingInvoice> invoices = grouped.getOrDefault(bucketName, List.of());
                long count = invoices.size();
                BigDecimal total = invoices.stream()
                    .map(ReportRepository.OutstandingInvoice::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new AgingBucket(bucketName, count, total);
            })
            .toList();
    }

    private static BigDecimal calculateTotalOutstanding(List<ReportRepository.OutstandingInvoice> outstanding) {
        return outstanding.stream()
            .map(ReportRepository.OutstandingInvoice::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Map<String, Long> toStatusMap(java.util.List<ReportRepository.StatusCount> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ReportRepository.StatusCount c : counts) {
            result.put(c.status(), c.count());
        }
        return result;
    }
}