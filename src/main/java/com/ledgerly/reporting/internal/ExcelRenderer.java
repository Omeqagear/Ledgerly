package com.ledgerly.reporting.internal;

import com.ledgerly.reporting.AgingBucket;
import com.ledgerly.reporting.AgingReport;
import com.ledgerly.reporting.CustomerSummary;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Renders report data to {@code .xlsx} workbooks using Apache POI. Styling
 * primitives (header style, currency style) are created once per workbook and
 * reused so a single renderer instance is thread-safe as long as each call
 * uses its own workbook.
 */
@Component
public class ExcelRenderer {

    private static final String HEADER_SHEET_SUMMARY = "Summary";
    private static final String HEADER_SHEET_AGING = "Aging Summary";
    private static final String HEADER_SHEET_CUSTOMER = "Customer Summary";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Renders the ledger-wide summary to an Excel workbook.
     *
     * @param output            destination stream (not closed by this method)
     * @param totalCustomers    number of customers tracked
     * @param invoicesByStatus  invoice counts grouped by status
     * @param paymentsByStatus  payment counts grouped by status
     * @param totalPaid         sum of all completed payment amounts
     * @param outstanding       sum of totalAmount for outstanding invoices
     */
    public void renderSummaryExcel(OutputStream output, long totalCustomers,
                                   Map<String, Long> invoicesByStatus,
                                   Map<String, Long> paymentsByStatus,
                                   BigDecimal totalPaid, BigDecimal outstanding) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(HEADER_SHEET_SUMMARY);
            Styles styles = new Styles(workbook);

            int rowIdx = 0;
            rowIdx = writeMetadataRow(sheet, rowIdx, styles, "Generated", LocalDate.now().format(DATE_FORMATTER));
            rowIdx = writeMetadataRow(sheet, rowIdx, styles, "Date Range", "All Time");

            rowIdx++; // blank separator row

            int headerRowIdx = rowIdx;
            rowIdx = writeHeaderRow(sheet, rowIdx, styles, "Metric", "Value");

            rowIdx = writeDataRow(sheet, rowIdx, styles, "Total Customers", String.valueOf(totalCustomers));
            for (Map.Entry<String, Long> entry : invoicesByStatus.entrySet()) {
                rowIdx = writeDataRow(sheet, rowIdx, styles,
                    "Invoices - " + entry.getKey(), String.valueOf(entry.getValue()));
            }
            for (Map.Entry<String, Long> entry : paymentsByStatus.entrySet()) {
                rowIdx = writeDataRow(sheet, rowIdx, styles,
                    "Payments - " + entry.getKey(), String.valueOf(entry.getValue()));
            }
            rowIdx = writeDataRow(sheet, rowIdx, styles, "Total Paid", formatAmount(totalPaid), true);
            rowIdx = writeDataRow(sheet, rowIdx, styles, "Outstanding", formatAmount(outstanding), true);

            sheet.createFreezePane(0, headerRowIdx + 1);
            autoSizeColumns(sheet, 2);

            writeWorkbook(workbook, output);
        }
    }

    /**
     * Renders an accounts-receivable aging report to an Excel workbook.
     *
     * @param output       destination stream (not closed by this method)
     * @param agingReport the aging report to render
     */
    public void renderAgingExcel(OutputStream output, AgingReport agingReport) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(HEADER_SHEET_AGING);
            Styles styles = new Styles(workbook);

            int rowIdx = 0;
            rowIdx = writeHeaderRow(sheet, rowIdx, styles, "Bucket", "Invoice Count", "Total Amount");

            for (AgingBucket bucket : agingReport.buckets()) {
                rowIdx = writeAgingBucketRow(sheet, rowIdx, styles, bucket);
            }

            // Total outstanding row
            Row totalRow = sheet.createRow(rowIdx++);
            Cell labelCell = totalRow.createCell(0);
            labelCell.setCellValue("Total Outstanding");
            CellStyle boldStyle = styles.bold();
            labelCell.setCellStyle(boldStyle);

            Cell countCell = totalRow.createCell(1);
            countCell.setCellValue("");
            countCell.setCellStyle(boldStyle);

            Cell amountCell = totalRow.createCell(2);
            amountCell.setCellValue(formatAmount(agingReport.totalOutstanding()));
            amountCell.setCellStyle(styles.boldCurrency());

            sheet.createFreezePane(0, 1);
            autoSizeColumns(sheet, 3);

            writeWorkbook(workbook, output);
        }
    }

    /**
     * Renders a per-customer summary to an Excel workbook.
     *
     * @param output   destination stream (not closed by this method)
     * @param summary  the customer summary to render
     */
    public void renderCustomerSummaryExcel(OutputStream output, CustomerSummary summary) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(HEADER_SHEET_CUSTOMER);
            Styles styles = new Styles(workbook);

            int rowIdx = 0;
            rowIdx = writeMetadataRow(sheet, rowIdx, styles, "Customer", safeName(summary.customerName()));
            rowIdx = writeMetadataRow(sheet, rowIdx, styles, "Generated", LocalDate.now().format(DATE_FORMATTER));

            rowIdx++; // blank separator row

            int headerRowIdx = rowIdx;
            rowIdx = writeHeaderRow(sheet, rowIdx, styles, "Metric", "Value");

            rowIdx = writeDataRow(sheet, rowIdx, styles, "Invoice Count", String.valueOf(summary.invoiceCount()));
            rowIdx = writeDataRow(sheet, rowIdx, styles, "Paid Invoice Count", String.valueOf(summary.paidInvoiceCount()));
            rowIdx = writeDataRow(sheet, rowIdx, styles, "Total Invoiced", formatAmount(summary.totalInvoiced()), true);
            rowIdx = writeDataRow(sheet, rowIdx, styles, "Total Paid", formatAmount(summary.totalPaid()), true);
            rowIdx = writeDataRow(sheet, rowIdx, styles, "Outstanding", formatAmount(summary.totalOutstanding()), true);

            sheet.createFreezePane(0, headerRowIdx + 1);
            autoSizeColumns(sheet, 2);

            writeWorkbook(workbook, output);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private int writeMetadataRow(Sheet sheet, int rowIdx, Styles styles, String label, String value) {
        Row row = sheet.createRow(rowIdx++);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label());
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        return rowIdx;
    }

    private int writeHeaderRow(Sheet sheet, int rowIdx, Styles styles, String... headers) {
        Row row = sheet.createRow(rowIdx++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header());
        }
        return rowIdx;
    }

    private int writeDataRow(Sheet sheet, int rowIdx, Styles styles, String metric, String value) {
        return writeDataRow(sheet, rowIdx, styles, metric, value, false);
    }

    private int writeDataRow(Sheet sheet, int rowIdx, Styles styles, String metric, String value, boolean currency) {
        Row row = sheet.createRow(rowIdx++);
        Cell metricCell = row.createCell(0);
        metricCell.setCellValue(metric);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        if (currency) {
            valueCell.setCellStyle(styles.currency());
        }
        return rowIdx;
    }

    private int writeAgingBucketRow(Sheet sheet, int rowIdx, Styles styles, AgingBucket bucket) {
        Row row = sheet.createRow(rowIdx++);
        Cell nameCell = row.createCell(0);
        nameCell.setCellValue(bucket.name());

        Cell countCell = row.createCell(1);
        countCell.setCellValue(bucket.invoiceCount());

        Cell amountCell = row.createCell(2);
        amountCell.setCellValue(formatAmount(bucket.totalAmount()));
        amountCell.setCellStyle(styles.currency());
        return rowIdx;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeWorkbook(Workbook workbook, OutputStream output) throws Exception {
        workbook.write(output);
        output.flush();
    }

    private String formatAmount(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).toPlainString();
    }

    private String safeName(String name) {
        return name == null ? "" : name;
    }

    // ------------------------------------------------------------------
    // Styles
    // ------------------------------------------------------------------

    /**
     * Lazily-created, per-workbook style cache. A {@link Workbook} is not
     * thread-safe, but each render call creates its own workbook and styles,
     * so the renderer itself remains stateless and thread-safe.
     */
    private static final class Styles {
        private final CellStyle header;
        private final CellStyle label;
        private final CellStyle bold;
        private final CellStyle currency;
        private final CellStyle boldCurrency;

        Styles(Workbook workbook) {
            this.header = createHeaderStyle(workbook);
            this.label = createBoldStyle(workbook);
            this.bold = createBoldStyle(workbook);
            this.currency = createCurrencyStyle(workbook, false);
            this.boldCurrency = createCurrencyStyle(workbook, true);
        }

        CellStyle header() { return header; }
        CellStyle label() { return label; }
        CellStyle bold() { return bold; }
        CellStyle currency() { return currency; }
        CellStyle boldCurrency() { return boldCurrency; }

        private static CellStyle createHeaderStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }

        private static CellStyle createBoldStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            return style;
        }

        private static CellStyle createCurrencyStyle(Workbook workbook, boolean bold) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(bold);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.RIGHT);
            return style;
        }
    }
}