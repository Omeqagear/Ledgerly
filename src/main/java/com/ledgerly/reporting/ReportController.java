package com.ledgerly.reporting;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/summary")
    public OverallSummary summary() {
        return reportService.overallSummary();
    }

    @GetMapping("/customers/{customerId}")
    public CustomerSummary customer(@PathVariable UUID customerId) {
        return reportService.customerSummary(customerId);
    }
}