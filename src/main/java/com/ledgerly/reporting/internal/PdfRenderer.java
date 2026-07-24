package com.ledgerly.reporting.internal;

import com.ledgerly.invoice.InvoiceStatus;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.util.List;

/**
 * Renders an invoice to a single-page PDF using OpenPDF. Layout primitives
 * (colors, fonts) are constants so the visual style is centralized here.
 */
@Component
public class PdfRenderer {

    private static final float MARGIN = 56.7f;

    private static final Color PRIMARY = new Color(0x2C, 0x3E, 0x50);
    private static final Color ACCENT = new Color(0x7F, 0x8C, 0x8D);
    private static final Color HEADER_BG = new Color(0xD6, 0xEA, 0xF8);
    private static final Color ROW_ALT = new Color(0xF2, 0xF3, 0xF4);
    private static final Color STATUS_PAID = new Color(0x27, 0xAE, 0x60);
    private static final Color STATUS_OVERDUE = new Color(0xE7, 0x4C, 0x3C);
    private static final Color STATUS_ISSUED = new Color(0x34, 0x98, 0xDB);
    private static final Color STATUS_DEFAULT = new Color(0x80, 0x80, 0x80);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 16, Font.NORMAL, ACCENT);
    private static final Font SECTION_LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font INFO_LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, PRIMARY);
    private static final Font TOTAL_LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);
    private static final Font TOTAL_VALUE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

    public void renderInvoicePdf(OutputStream output, String customerName, String invoiceNumber,
                                  String issueDate, String dueDate, String subtotal,
                                  String tax, String total, InvoiceStatus status) throws DocumentException {
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter.getInstance(document, output);
        document.open();
        try {
            addHeader(document);
            addInvoiceInfo(document, invoiceNumber, issueDate, dueDate);
            addSpacer(document);
            addBillTo(document, customerName);
            addLineItems(document);
            addSpacer(document);
            addTotals(document, subtotal, tax, total, status);
        } finally {
            document.close();
        }
    }

    private void addHeader(Document document) throws DocumentException {
        Paragraph title = new Paragraph("Ledgerly", TITLE_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        Paragraph subtitle = new Paragraph("INVOICE", SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_LEFT);
        subtitle.setSpacingAfter(24f);
        document.add(subtitle);
    }

    private void addInvoiceInfo(Document document, String invoiceNumber, String issueDate,
                                String dueDate) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(infoLabelCell("Invoice Number"));
        table.addCell(infoValueCell(invoiceNumber));
        table.addCell(infoLabelCell("Issue Date"));
        table.addCell(infoValueCell(issueDate));
        table.addCell(infoLabelCell("Due Date"));
        table.addCell(infoValueCell(dueDate));
        document.add(table);
    }

    private void addBillTo(Document document, String customerName) throws DocumentException {
        Paragraph label = new Paragraph("Bill To:", SECTION_LABEL_FONT);
        label.setSpacingAfter(4f);
        document.add(label);
        Paragraph name = new Paragraph(safe(customerName), BODY_FONT);
        name.setSpacingAfter(24f);
        document.add(name);
    }

    private void addLineItems(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 1.5f, 2f, 2f});
        table.addCell(headerCell("Description", Element.ALIGN_LEFT));
        table.addCell(headerCell("Quantity", Element.ALIGN_RIGHT));
        table.addCell(headerCell("Unit Price", Element.ALIGN_RIGHT));
        table.addCell(headerCell("Amount", Element.ALIGN_RIGHT));
        // The summary contract does not carry individual line items; the table
        // is emitted with its header row so downstream styling remains intact.
        document.add(table);
    }

    private void addTotals(Document document, String subtotal, String tax, String total,
                           InvoiceStatus status) throws DocumentException {
        Color statusColor = statusColor(status);
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalsLabelCell("Subtotal"));
        table.addCell(totalsValueCell(safe(subtotal)));
        table.addCell(totalsLabelCell("Tax"));
        table.addCell(totalsValueCell(safe(tax)));
        table.addCell(totalsLabelCell("Total"));
        Font totalFont = new Font(Font.HELVETICA, 11, Font.BOLD, statusColor);
        PdfPCell totalCell = new PdfPCell(new Phrase(
            safe(total) + "  [" + (status != null ? status.name() : "") + "]", totalFont));
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setPadding(4f);
        table.addCell(totalCell);
        document.add(table);
    }

    private void addSpacer(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        document.add(spacer);
    }

    private PdfPCell infoLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, INFO_LABEL_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell infoValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), BODY_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell headerCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        return cell;
    }

    private PdfPCell totalsLabelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TOTAL_LABEL_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell totalsValueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TOTAL_VALUE_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4f);
        return cell;
    }

    private Color statusColor(InvoiceStatus status) {
        if (status == null) {
            return STATUS_DEFAULT;
        }
        switch (status) {
            case PAID: return STATUS_PAID;
            case OVERDUE: return STATUS_OVERDUE;
            case ISSUED: return STATUS_ISSUED;
            default: return STATUS_DEFAULT;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record TransactionRow(String date, String description, String invoiceNumber, String amount, String status) {}

    public void renderCustomerStatementPdf(OutputStream output, String customerName, String statementPeriod,
                                            String totalInvoiced, String totalPaid, String outstanding,
                                            List<TransactionRow> transactions) throws DocumentException {
        Document document = new Document(PageSize.A4, 56.7f, 56.7f, 56.7f, 56.7f);
        PdfWriter.getInstance(document, output);
        document.open();
        try {
            // Header section
            Paragraph title = new Paragraph("Ledgerly", TITLE_FONT);
            document.add(title);
            Font subtitleFont = new Font(Font.HELVETICA, 16, Font.BOLD, ACCENT);
            Paragraph subtitle = new Paragraph("CUSTOMER STATEMENT", subtitleFont);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            document.add(new Paragraph("Customer: " + safe(customerName), BODY_FONT));
            document.add(new Paragraph("Statement Period: " + safe(statementPeriod), BODY_FONT));
            addSpacer(document);

            // Summary section
            Paragraph section = new Paragraph("Summary", SECTION_LABEL_FONT);
            section.setSpacingAfter(10);
            document.add(section);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(60);
            summaryTable.setWidths(new float[]{1, 1});
            addTotalRow(summaryTable, "Total Invoiced:", totalInvoiced);
            addTotalRow(summaryTable, "Total Paid:", totalPaid);
            addTotalRow(summaryTable, "Outstanding:", outstanding);
            document.add(summaryTable);
            addSpacer(document);

            // Transaction table
            Paragraph transSection = new Paragraph("Transactions", SECTION_LABEL_FONT);
            transSection.setSpacingAfter(10);
            document.add(transSection);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2, 1.5f, 1, 1});
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            addTableHeader(table, "Date", headerFont);
            addTableHeader(table, "Description", headerFont);
            addTableHeader(table, "Invoice #", headerFont);
            addTableHeader(table, "Amount", headerFont);
            addTableHeader(table, "Status", headerFont);

            Font rowFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            boolean altRow = false;
            for (TransactionRow row : transactions) {
                Color bgColor = altRow ? ROW_ALT : Color.WHITE;
                PdfPCell cell;
                cell = new PdfPCell(new Phrase(row.date(), rowFont)); cell.setBackgroundColor(bgColor); cell.setPadding(8); table.addCell(cell);
                cell = new PdfPCell(new Phrase(row.description(), rowFont)); cell.setBackgroundColor(bgColor); cell.setPadding(8); table.addCell(cell);
                cell = new PdfPCell(new Phrase(row.invoiceNumber(), rowFont)); cell.setBackgroundColor(bgColor); cell.setPadding(8); table.addCell(cell);
                cell = new PdfPCell(new Phrase(row.amount(), rowFont)); cell.setBackgroundColor(bgColor); cell.setPadding(8); cell.setHorizontalAlignment(Element.ALIGN_RIGHT); table.addCell(cell);
                cell = new PdfPCell(new Phrase(row.status(), rowFont)); cell.setBackgroundColor(bgColor); cell.setPadding(8); cell.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(cell);
                altRow = !altRow;
            }
            document.add(table);

            // Footer
            addSpacer(document);
            Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, ACCENT);
            Paragraph footer = new Paragraph("Generated: " + java.time.LocalDateTime.now().toString(), footerFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);
        } finally {
            document.close();
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, TOTAL_LABEL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setPadding(4f);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(safe(value), TOTAL_VALUE_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4f);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PRIMARY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }
}