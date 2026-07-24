package com.ledgerly.reporting.internal;

import com.lowagie.text.DocumentException;
import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.reporting.CustomerSummary;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfService {
    private final InvoiceDataProvider invoiceDataProvider;
    private final PdfRenderer pdfRenderer;
    private final ReportRepository reportRepository;
    private final CustomerService customerService;
    private final ReportGenerator reportGenerator;

    public PdfService(InvoiceDataProvider invoiceDataProvider, PdfRenderer pdfRenderer,
                      ReportRepository reportRepository, CustomerService customerService,
                      ReportGenerator reportGenerator) {
        this.invoiceDataProvider = invoiceDataProvider;
        this.pdfRenderer = pdfRenderer;
        this.reportRepository = reportRepository;
        this.customerService = customerService;
        this.reportGenerator = reportGenerator;
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

    public void generateCustomerStatementPdf(OutputStream output, UUID customerId) {
        Customer customer = customerService.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
        CustomerSummary summary = reportGenerator.forCustomer(customerId);
        List<PdfRenderer.TransactionRow> transactions = fetchCustomerTransactions(customerId);
        try {
            pdfRenderer.renderCustomerStatementPdf(output, customer.getName(), "All Time",
                formatAmount(summary.totalInvoiced()), formatAmount(summary.totalPaid()),
                formatAmount(summary.totalOutstanding()), transactions);
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private List<PdfRenderer.TransactionRow> fetchCustomerTransactions(UUID customerId) {
        List<PdfRenderer.TransactionRow> transactions = new ArrayList<>();

        for (ReportRepository.CustomerInvoice inv : reportRepository.findCustomerInvoices(customerId)) {
            transactions.add(new PdfRenderer.TransactionRow(
                inv.issueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "Invoice", inv.invoiceNumber(), formatAmount(inv.totalAmount()), inv.status()
            ));
        }

        for (ReportRepository.CustomerPayment pay : reportRepository.findCustomerPayments(customerId)) {
            String date = pay.processedAt() != null
                ? pay.processedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : "";
            transactions.add(new PdfRenderer.TransactionRow(
                date, "Payment", "", formatAmount(pay.amount()), pay.status()
            ));
        }

        transactions.sort((a, b) -> b.date().compareTo(a.date()));
        return transactions;
    }

    private String formatDate(java.time.LocalDate date) { return date.format(DateTimeFormatter.ISO_LOCAL_DATE); }
    private String formatAmount(java.math.BigDecimal amount) { return amount.toPlainString(); }
}