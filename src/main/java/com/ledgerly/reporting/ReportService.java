package com.ledgerly.reporting;

import org.springframework.cache.annotation.Cacheable;
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
    private final com.ledgerly.reporting.internal.PdfService pdfService;
    private final com.ledgerly.reporting.internal.ExcelService excelService;

    public ReportService(com.ledgerly.reporting.internal.ReportGenerator reportGenerator,
                         com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator,
                         com.ledgerly.reporting.internal.PdfService pdfService,
                         com.ledgerly.reporting.internal.ExcelService excelService) {
        this.reportGenerator = reportGenerator;
        this.dateRangeValidator = dateRangeValidator;
        this.pdfService = pdfService;
        this.excelService = excelService;
    }

    public OverallSummary overallSummary() {
        return overallSummary(null, null);
    }

    @Cacheable(value = "reports", key = "'summary-' + (#from != null ? #from : 'all') + '-' + (#to != null ? #to : 'all')")
    public OverallSummary overallSummary(LocalDate from, LocalDate to) {
        dateRangeValidator.validate(from, to);
        return reportGenerator.overall(from, to);
    }

    @Cacheable(value = "reports", key = "'cust-summary-' + #customerId")
    public CustomerSummary customerSummary(UUID customerId) {
        return reportGenerator.forCustomer(customerId);
    }

    @Cacheable(value = "reports", key = "'aging'")
    public AgingReport agingReport() {
        return reportGenerator.agingReport();
    }

    @Cacheable(value = "reports", key = "'aging-cust-' + #customerId")
    public AgingReport agingReportForCustomer(UUID customerId) {
        return reportGenerator.agingReportForCustomer(customerId);
    }

    public void generateInvoicePdf(java.io.OutputStream output, java.util.UUID invoiceId) {
        pdfService.generateInvoicePdf(output, invoiceId);
    }

    public void generateCustomerStatementPdf(java.io.OutputStream output, java.util.UUID customerId) {
        pdfService.generateCustomerStatementPdf(output, customerId);
    }

    public void generateSummaryExcel(java.io.OutputStream output, LocalDate from, LocalDate to) {
        dateRangeValidator.validate(from, to);
        excelService.generateSummaryExcel(output, from, to);
    }

    public void generateAgingExcel(java.io.OutputStream output) {
        excelService.generateAgingExcel(output);
    }

    public void generateCustomerSummaryExcel(java.io.OutputStream output, UUID customerId) {
        excelService.generateCustomerSummaryExcel(output, customerId);
    }
}