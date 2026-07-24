package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.AgingBucket;
import com.ledgerly.reporting.AgingReport;
import com.ledgerly.reporting.CustomerSummary;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelRendererTest {

    private final ExcelRenderer renderer = new ExcelRenderer();

    @Test
    void summaryExcelShouldProduceValidXlsx() throws Exception {
        Map<String, Long> invoicesByStatus = new LinkedHashMap<>();
        invoicesByStatus.put("ISSUED", 3L);
        invoicesByStatus.put("PAID", 5L);

        Map<String, Long> paymentsByStatus = new LinkedHashMap<>();
        paymentsByStatus.put("COMPLETED", 4L);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderSummaryExcel(out, 10L, invoicesByStatus, paymentsByStatus,
            new BigDecimal("1200.00"), new BigDecimal("3000.00"));

        assertThat(out.toByteArray()).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetName(0)).isEqualTo("Summary");
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isGreaterThan(0);
        }
    }

    @Test
    void agingExcelShouldHaveCorrectSheetNameAndRowCount() throws Exception {
        List<AgingBucket> buckets = List.of(
            new AgingBucket("Current", 2L, new BigDecimal("500.00")),
            new AgingBucket("1-30 days", 1L, new BigDecimal("250.00")),
            new AgingBucket("31-60 days", 0L, BigDecimal.ZERO),
            new AgingBucket("61-90 days", 0L, BigDecimal.ZERO),
            new AgingBucket("90+ days", 0L, BigDecimal.ZERO)
        );
        AgingReport agingReport = new AgingReport(buckets, new BigDecimal("750.00"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderAgingExcel(out, agingReport);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Aging Summary");
            // 1 header row + 5 bucket rows + 1 total row = 7
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(7);
        }
    }

    @Test
    void customerSummaryExcelShouldHaveCorrectSheetName() throws Exception {
        CustomerSummary summary = new CustomerSummary(
            UUID.randomUUID(), "Acme Corp", 8L, 5L,
            new BigDecimal("10000.00"), new BigDecimal("7000.00"),
            new BigDecimal("3000.00")
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderCustomerSummaryExcel(out, summary);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Customer Summary");
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isGreaterThan(0);
        }
    }
}