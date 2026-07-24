package com.ledgerly.reporting.internal;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceNotFoundException;
import com.ledgerly.invoice.InvoiceService;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class InvoiceDataProvider {
    private final InvoiceService invoiceService;
    private final CustomerService customerService;

    public InvoiceDataProvider(InvoiceService invoiceService, CustomerService customerService) {
        this.invoiceService = invoiceService;
        this.customerService = customerService;
    }

    public InvoiceWithCustomer fetchInvoiceWithCustomer(UUID invoiceId) {
        Invoice invoice = invoiceService.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        Customer customer = customerService.findById(invoice.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(invoice.getCustomerId()));
        return new InvoiceWithCustomer(invoice, customer);
    }

    public record InvoiceWithCustomer(Invoice invoice, Customer customer) {}
}