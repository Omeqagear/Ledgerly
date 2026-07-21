package com.ledgerly.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Public service for the reporting module. Read-only and safe to call without
 * an open transaction.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final com.ledgerly.reporting.internal.ReportGenerator reportGenerator;

    public ReportService(com.ledgerly.reporting.internal.ReportGenerator reportGenerator) {
        this.reportGenerator = reportGenerator;
    }

    public OverallSummary overallSummary() {
        return reportGenerator.overall();
    }

    public CustomerSummary customerSummary(UUID customerId) {
        return reportGenerator.forCustomer(customerId);
    }
}