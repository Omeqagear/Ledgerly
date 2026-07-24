package com.ledgerly.reporting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(
    value = BootstrapMode.DIRECT_DEPENDENCIES,
    extraIncludes = {"payment"})
class ReportModuleIntegrationTests {

    @Autowired
    private ReportService reportService;

    @Test
    void shouldReturnAllTimeSummary() {
        OverallSummary summary = reportService.overallSummary();
        assertThat(summary).isNotNull();
    }

    @Test
    void shouldReturnFilteredSummary() {
        OverallSummary summary = reportService.overallSummary(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(summary).isNotNull();
    }

    @Test
    void shouldReturnAgingReport() {
        AgingReport report = reportService.agingReport();
        assertThat(report).isNotNull();
        assertThat(report.buckets()).hasSize(5);
    }

    @Test
    void shouldReturnCustomerAgingReport() {
        AgingReport report = reportService.agingReportForCustomer(UUID.randomUUID());
        assertThat(report).isNotNull();
        assertThat(report.buckets()).hasSize(5);
    }

    @Test
    void shouldGenerateSummaryExcel() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        reportService.generateSummaryExcel(output, null, null);
        assertThat(output.toByteArray()).isNotEmpty();
    }

    @Test
    void shouldGenerateAgingExcel() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        reportService.generateAgingExcel(output);
        assertThat(output.toByteArray()).isNotEmpty();
    }
}