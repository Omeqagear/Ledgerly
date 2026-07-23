package com.ledgerly.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public service for the reporting module. Read-only and safe to call without
 * an open transaction.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final com.ledgerly.reporting.internal.ReportGenerator reportGenerator;
    private final com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator;

    public ReportService(com.ledgerly.reporting.internal.ReportGenerator reportGenerator,
                         com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator) {
        this.reportGenerator = reportGenerator;
        this.dateRangeValidator = dateRangeValidator;
    }

    public OverallSummary overallSummary() {
        return overallSummary(null, null);
    }

    public OverallSummary overallSummary(LocalDate from, LocalDate to) {
        dateRangeValidator.validate(from, to);
        return reportGenerator.overall(from, to);
    }

    public CustomerSummary customerSummary(UUID customerId) {
        return reportGenerator.forCustomer(customerId);
    }

    public AgingReport agingReport() {
        return reportGenerator.agingReport();
    }

    public AgingReport agingReportForCustomer(UUID customerId) {
        return reportGenerator.agingReportForCustomer(customerId);
    }
}