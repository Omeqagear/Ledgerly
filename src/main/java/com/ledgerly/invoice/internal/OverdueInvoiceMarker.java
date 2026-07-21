package com.ledgerly.invoice.internal;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Daily job that transitions issued invoices past their due date to
 * {@link com.ledgerly.invoice.InvoiceStatus#OVERDUE}.
 */
@Component
class OverdueInvoiceMarker {

    private final InvoiceRepository invoiceRepository;

    OverdueInvoiceMarker(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> overdue = invoiceRepository.findOverdueInvoices();
        for (Invoice invoice : overdue) {
            if (invoice.getStatus() == InvoiceStatus.ISSUED) {
                invoice.markAsOverdue();
                invoiceRepository.save(invoice);
            }
        }
    }
}
