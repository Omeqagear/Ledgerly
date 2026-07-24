package com.ledgerly.reporting.internal;

import com.lowagie.text.DocumentException;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceStatus;
import com.ledgerly.customer.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private InvoiceDataProvider invoiceDataProvider;

    @Mock
    private PdfRenderer pdfRenderer;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private com.ledgerly.customer.CustomerService customerService;

    @Mock
    private ReportGenerator reportGenerator;

    private PdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfService(invoiceDataProvider, pdfRenderer, reportRepository, customerService, reportGenerator);
    }

    @Test
    void shouldDelegateFormattedValuesToRenderer() throws DocumentException {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoice.getInvoiceNumber()).thenReturn("INV-2026-000001");
        when(invoice.getIssueDate()).thenReturn(LocalDate.of(2026, 7, 1));
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2026, 7, 31));
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("1200.00"));
        when(invoice.getTaxAmount()).thenReturn(new BigDecimal("200.00"));
        when(invoice.getStatus()).thenReturn(InvoiceStatus.ISSUED);

        Customer customer = mock(Customer.class);
        when(customer.getName()).thenReturn("Acme Corp");

        when(invoiceDataProvider.fetchInvoiceWithCustomer(invoiceId))
            .thenReturn(new InvoiceDataProvider.InvoiceWithCustomer(invoice, customer));

        OutputStream output = mock(OutputStream.class);

        pdfService.generateInvoicePdf(output, invoiceId);

        verify(pdfRenderer).renderInvoicePdf(
            eq(output), eq("Acme Corp"), eq("INV-2026-000001"),
            eq("2026-07-01"), eq("2026-07-31"),
            eq("1200.00"), eq("200.00"), eq("1200.00"), eq(InvoiceStatus.ISSUED));
    }

    @Test
    void shouldWrapDocumentExceptionAsRuntimeException() throws DocumentException {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoice.getInvoiceNumber()).thenReturn("INV-2026-000002");
        when(invoice.getIssueDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2026, 6, 30));
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("600.00"));
        when(invoice.getTaxAmount()).thenReturn(new BigDecimal("100.00"));
        when(invoice.getStatus()).thenReturn(InvoiceStatus.PAID);

        Customer customer = mock(Customer.class);
        when(customer.getName()).thenReturn("Globex Inc.");

        when(invoiceDataProvider.fetchInvoiceWithCustomer(invoiceId))
            .thenReturn(new InvoiceDataProvider.InvoiceWithCustomer(invoice, customer));

        OutputStream output = mock(OutputStream.class);
        doThrow(new DocumentException("boom"))
            .when(pdfRenderer).renderInvoicePdf(any(), any(), any(), any(), any(),
                any(), any(), any(), any(InvoiceStatus.class));

        assertThatThrownBy(() -> pdfService.generateInvoicePdf(output, invoiceId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to generate PDF")
            .hasRootCauseInstanceOf(DocumentException.class);
    }
}