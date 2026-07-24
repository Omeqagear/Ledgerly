package com.ledgerly.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.test.ApplicationModuleTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@ApplicationModuleTest
class CustomerPaginationIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Test
    void shouldPaginateFindAll() {
        for (int i = 0; i < 5; i++) {
            customerService.createCustomer("C" + i, "c" + i + "@example.com", null, null);
        }

        Page<Customer> page = customerService.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(3);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void shouldSortByNameAscending() {
        customerService.createCustomer("Zebra", "zebra@example.com", null, null);
        customerService.createCustomer("Alpha", "alpha@example.com", null, null);

        Page<Customer> page = customerService.findAll(
            PageRequest.of(0, 10, Sort.by("name").ascending()));

        assertThat(page.getContent()).isNotEmpty();
        List<String> names = page.getContent().stream().map(Customer::getName).toList();
        assertThat(names).contains("Alpha", "Zebra");
        int alphaIndex = names.indexOf("Alpha");
        int zebraIndex = names.indexOf("Zebra");
        assertThat(alphaIndex).isLessThan(zebraIndex);
    }
}
