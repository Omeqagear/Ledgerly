package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.AgingReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorAgingTest {

    @Mock
    private ReportRepository reportRepository;

    private ReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator(reportRepository, new AgingCalculator());
    }

    @Test
    void shouldProduceFiveBucketsInFixedOrder() {
        LocalDate today = LocalDate.now();
        when(reportRepository.findOutstandingInvoices()).thenReturn(List.of(
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-001", new BigDecimal("100.00"), today.plusDays(5)),
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-002", new BigDecimal("200.00"), today.minusDays(15)),
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-003", new BigDecimal("300.00"), today.minusDays(45)),
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-004", new BigDecimal("400.00"), today.minusDays(75)),
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-005", new BigDecimal("500.00"), today.minusDays(100))
        ));

        AgingReport report = reportGenerator.agingReport();

        assertThat(report.buckets()).hasSize(5);
        assertThat(report.buckets().get(0).name()).isEqualTo("Current");
        assertThat(report.buckets().get(0).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(0).totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        assertThat(report.buckets().get(1).name()).isEqualTo("1-30 days");
        assertThat(report.buckets().get(1).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(1).totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));

        assertThat(report.buckets().get(2).name()).isEqualTo("31-60 days");
        assertThat(report.buckets().get(2).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(2).totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));

        assertThat(report.buckets().get(3).name()).isEqualTo("61-90 days");
        assertThat(report.buckets().get(3).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(3).totalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));

        assertThat(report.buckets().get(4).name()).isEqualTo("90+ days");
        assertThat(report.buckets().get(4).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(4).totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

        assertThat(report.totalOutstanding()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void shouldReturnAllBucketsWithZerosWhenNoOutstandingInvoices() {
        when(reportRepository.findOutstandingInvoices()).thenReturn(List.of());

        AgingReport report = reportGenerator.agingReport();

        assertThat(report.buckets()).hasSize(5);
        assertThat(report.buckets()).allSatisfy(bucket -> {
            assertThat(bucket.invoiceCount()).isZero();
            assertThat(bucket.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(report.totalOutstanding()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldIncludeAllFiveBucketsEvenWhenSomeAreEmpty() {
        LocalDate today = LocalDate.now();
        when(reportRepository.findOutstandingInvoices()).thenReturn(List.of(
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-001", new BigDecimal("100.00"), today.plusDays(5)),
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-002", new BigDecimal("500.00"), today.minusDays(100))
        ));

        AgingReport report = reportGenerator.agingReport();

        assertThat(report.buckets()).hasSize(5);
        assertThat(report.buckets()).extracting(b -> b.name())
            .containsExactly("Current", "1-30 days", "31-60 days", "61-90 days", "90+ days");
        assertThat(report.buckets().get(0).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(1).invoiceCount()).isZero();
        assertThat(report.buckets().get(2).invoiceCount()).isZero();
        assertThat(report.buckets().get(3).invoiceCount()).isZero();
        assertThat(report.buckets().get(4).invoiceCount()).isEqualTo(1);
        assertThat(report.totalOutstanding()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void shouldGroupMultipleInvoicesInSameBucket() {
        LocalDate today = LocalDate.now();
        when(reportRepository.findOutstandingInvoices()).thenReturn(List.of(
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-001", new BigDecimal("100.00"), today.minusDays(10)),
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), UUID.randomUUID(), "INV-002", new BigDecimal("200.00"), today.minusDays(20))
        ));

        AgingReport report = reportGenerator.agingReport();

        assertThat(report.buckets().get(1).name()).isEqualTo("1-30 days");
        assertThat(report.buckets().get(1).invoiceCount()).isEqualTo(2);
        assertThat(report.buckets().get(1).totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(report.totalOutstanding()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void shouldFilterByCustomerForCustomerAgingReport() {
        UUID customerId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        when(reportRepository.findOutstandingInvoicesForCustomer(customerId)).thenReturn(List.of(
            new ReportRepository.OutstandingInvoice(UUID.randomUUID(), customerId, "INV-001", new BigDecimal("250.00"), today.minusDays(20))
        ));

        AgingReport report = reportGenerator.agingReportForCustomer(customerId);

        assertThat(report.buckets()).hasSize(5);
        assertThat(report.buckets().get(1).name()).isEqualTo("1-30 days");
        assertThat(report.buckets().get(1).invoiceCount()).isEqualTo(1);
        assertThat(report.buckets().get(1).totalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(report.totalOutstanding()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void shouldReturnZerosForCustomerAgingReportWhenNoOutstandingInvoices() {
        UUID customerId = UUID.randomUUID();
        when(reportRepository.findOutstandingInvoicesForCustomer(customerId)).thenReturn(List.of());

        AgingReport report = reportGenerator.agingReportForCustomer(customerId);

        assertThat(report.buckets()).hasSize(5);
        assertThat(report.buckets()).allSatisfy(bucket -> {
            assertThat(bucket.invoiceCount()).isZero();
            assertThat(bucket.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(report.totalOutstanding()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}