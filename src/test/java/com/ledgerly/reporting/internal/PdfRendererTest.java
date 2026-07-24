package com.ledgerly.reporting.internal;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfRendererTest {

    private final PdfRenderer renderer = new PdfRenderer();

    @Test
    void shouldProduceValidMultiPageCountPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderInvoicePdf(out, "Acme Corp", "INV-2026-000001",
            "2026-07-01", "2026-07-31", "1000.00", "200.00", "1200.00", "ISSUED");

        assertThat(out.toByteArray()).isNotEmpty();
        assertValidPdf(out.toByteArray());
    }

    @Test
    void shouldProduceValidPdfForPaidStatus() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderInvoicePdf(out, "Globex Inc.", "INV-2026-000002",
            "2026-06-01", "2026-06-30", "500.00", "100.00", "600.00", "PAID");

        assertValidPdf(out.toByteArray());
    }

    @Test
    void shouldProduceValidPdfForOverdueStatus() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderInvoicePdf(out, "Initech", "INV-2026-000003",
            "2026-05-01", "2026-05-15", "750.00", "150.00", "900.00", "OVERDUE");

        assertValidPdf(out.toByteArray());
    }

    @Test
    void shouldHandleNullValuesGracefully() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.renderInvoicePdf(out, null, null, null, null, null, null, null, null);

        assertValidPdf(out.toByteArray());
    }

    private void assertValidPdf(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            assertThat(reader.getNumberOfPages()).isGreaterThan(0);
        } finally {
            reader.close();
        }
    }
}