package com.ledgerly.reporting;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for the {@code reporting} module. Purely read-only.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Returns the ledger-wide summary. The optional {@code from}/{@code to} parameters
     * filter only the invoice-by-status breakdown by issue date; all other metrics
     * (customer count, payment counts, paid/outstanding totals) are all-time.
     */
    @GetMapping("/summary")
    public OverallSummary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.overallSummary(from, to);
    }

    @GetMapping("/customers/{customerId}")
    public CustomerSummary customer(@PathVariable UUID customerId) {
        return reportService.customerSummary(customerId);
    }

    @GetMapping("/aging")
    public AgingReport aging() {
        return reportService.agingReport();
    }

    @GetMapping("/customers/{customerId}/aging")
    public AgingReport customerAging(@PathVariable UUID customerId) {
        return reportService.agingReportForCustomer(customerId);
    }

    @GetMapping("/invoices/{invoiceId}/pdf")
    public void invoicePdf(@PathVariable UUID invoiceId, HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"invoice-" + invoiceId + ".pdf\"");
        reportService.generateInvoicePdf(response.getOutputStream(), invoiceId);
    }
}