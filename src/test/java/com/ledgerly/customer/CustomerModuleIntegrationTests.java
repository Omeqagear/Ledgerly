package com.ledgerly.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ApplicationModuleTest
class CustomerModuleIntegrationTests {

    @Autowired
    private CustomerService customerService;

    @Test
    void shouldCreateCustomer() {
        Customer customer = customerService.createCustomer(
            "Acme Corp", "acme@example.com", "VAT-123", "1 Main St");

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getEmail()).isEqualTo("acme@example.com");
        assertThat(customer.getPreferredLanguage()).isEqualTo("en");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        customerService.createCustomer("Acme", "dup@example.com", null, null);

        assertThatThrownBy(() ->
            customerService.createCustomer("Acme2", "dup@example.com", null, null))
            .isInstanceOf(DuplicateCustomerEmailException.class);
    }

    @Test
    void shouldUseCustomerLookupForEmailResolution() {
        Customer customer = customerService.createCustomer(
            "Beta LLC", "beta@example.com", null, null);

        assertThat(customerService.existsById(customer.getId())).isTrue();
        assertThat(customerService.findById(customer.getId()))
            .isPresent()
            .get()
            .extracting(Customer::getEmail)
            .isEqualTo("beta@example.com");
    }
}