package com.ledgerly.reporting.internal;

import com.lowagie.text.DocumentException;
import com.ledgerly.customer.Customer;
import com.ledgerly.invoice.Invoice;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PdfService {
    private final InvoiceDataProvider invoiceDataProvider;
    private final PdfRenderer pdfRenderer;

    public PdfService(InvoiceDataProvider invoiceDataProvider, PdfRenderer pdfRenderer) {
        this.invoiceDataProvider = invoiceDataProvider;
        this.pdfRenderer = pdfRenderer;
    }

    public void generateInvoicePdf(OutputStream output, UUID invoiceId) {
        InvoiceDataProvider.InvoiceWithCustomer data = invoiceDataProvider.fetchInvoiceWithCustomer(invoiceId);
        Invoice invoice = data.invoice();
        Customer customer = data.customer();
        try {
            pdfRenderer.renderInvoicePdf(output, customer.getName(), invoice.getInvoiceNumber(),
                formatDate(invoice.getIssueDate()), formatDate(invoice.getDueDate()),
                formatAmount(invoice.getTotalAmount()), formatAmount(invoice.getTaxAmount()),
                formatAmount(invoice.getTotalAmount()), invoice.getStatus().name());
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String formatDate(java.time.LocalDate date) { return date.format(DateTimeFormatter.ISO_LOCAL_DATE); }
    private String formatAmount(java.math.BigDecimal amount) { return amount.toPlainString(); }
}