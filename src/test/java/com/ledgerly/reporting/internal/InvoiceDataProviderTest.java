package com.ledgerly.reporting.internal;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceNotFoundException;
import com.ledgerly.invoice.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceDataProviderTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private CustomerService customerService;

    private InvoiceDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InvoiceDataProvider(invoiceService, customerService);
    }

    @Test
    void shouldReturnInvoiceWithMatchingCustomer() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);
        when(invoice.getCustomerId()).thenReturn(customerId);
        when(invoiceService.findById(invoiceId)).thenReturn(Optional.of(invoice));

        Customer customer = mock(Customer.class);
        when(customerService.findById(customerId)).thenReturn(Optional.of(customer));

        InvoiceDataProvider.InvoiceWithCustomer result =
            provider.fetchInvoiceWithCustomer(invoiceId);

        assertThat(result.invoice()).isSameAs(invoice);
        assertThat(result.customer()).isSameAs(customer);
        verify(invoiceService).findById(invoiceId);
        verify(customerService).findById(customerId);
    }

    @Test
    void shouldThrowInvoiceNotFoundWhenInvoiceMissing() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.fetchInvoiceWithCustomer(invoiceId))
            .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void shouldThrowCustomerNotFoundWhenCustomerMissing() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);
        when(invoice.getCustomerId()).thenReturn(customerId);
        when(invoiceService.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerService.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.fetchInvoiceWithCustomer(invoiceId))
            .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void shouldUseInvoiceCustomerIdToLookupCustomer() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);
        when(invoice.getCustomerId()).thenReturn(customerId);
        when(invoiceService.findById(invoiceId)).thenReturn(Optional.of(invoice));

        Customer customer = mock(Customer.class);
        when(customerService.findById(customerId)).thenReturn(Optional.of(customer));

        provider.fetchInvoiceWithCustomer(invoiceId);

        verify(customerService).findById(customerId);
    }
}