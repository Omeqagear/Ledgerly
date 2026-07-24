package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.AgingReport;
import com.ledgerly.reporting.CustomerSummary;
import com.ledgerly.reporting.OverallSummary;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Orchestrates Excel report generation by fetching data through {@link
 * ReportGenerator} and rendering it with {@link ExcelRenderer}. Each method
 * is self-contained: it pulls the required data, renders the workbook, and
 * writes it to the supplied stream. Rendering failures are wrapped in a
 * {@link RuntimeException} so callers see a consistent error type.
 */
@Service
public class ExcelService {

    private final ExcelRenderer excelRenderer;
    private final ReportGenerator reportGenerator;

    public ExcelService(ExcelRenderer excelRenderer, ReportGenerator reportGenerator) {
        this.excelRenderer = excelRenderer;
        this.reportGenerator = reportGenerator;
    }

    public void generateSummaryExcel(OutputStream output, LocalDate from, LocalDate to) {
        OverallSummary summary = reportGenerator.overall(from, to);
        try {
            excelRenderer.renderSummaryExcel(
                output,
                summary.totalCustomers(),
                summary.invoicesByStatus(),
                summary.paymentsByStatus(),
                summary.totalAmountPaid(),
                summary.totalOutstanding());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    public void generateAgingExcel(OutputStream output) {
        AgingReport agingReport = reportGenerator.agingReport();
        try {
            excelRenderer.renderAgingExcel(output, agingReport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    public void generateCustomerSummaryExcel(OutputStream output, UUID customerId) {
        CustomerSummary summary = reportGenerator.forCustomer(customerId);
        try {
            excelRenderer.renderCustomerSummaryExcel(output, summary);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }
}