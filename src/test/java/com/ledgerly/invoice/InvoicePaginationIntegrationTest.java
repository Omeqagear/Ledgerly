package com.ledgerly.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Transactional
class InvoicePaginationIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void shouldPaginateFindAll() {
        UUID customerId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            invoiceService.createInvoice(
                customerId, new BigDecimal("100.00"), BigDecimal.ONE, LocalDate.now().plusDays(30));
        }

        Page<Invoice> page = invoiceService.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void shouldPaginateByCustomerId() {
        UUID customerId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            invoiceService.createInvoice(
                customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        }

        Page<Invoice> page = invoiceService.findByCustomerId(customerId, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void shouldSortByInvoiceNumberDescending() {
        UUID customerId = UUID.randomUUID();
        invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));

        Page<Invoice> page = invoiceService.findAll(
            PageRequest.of(0, 10, Sort.by("invoiceNumber").descending()));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getInvoiceNumber())
            .isGreaterThan(page.getContent().get(1).getInvoiceNumber());
    }

    @Test
    void shouldPaginateOverdueInvoices() {
        UUID customerId = UUID.randomUUID();
        Invoice issued = invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().minusDays(1));
        invoiceService.issueInvoice(issued.getId());

        Page<Invoice> page = invoiceService.findOverdueInvoices(PageRequest.of(0, 10));

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
