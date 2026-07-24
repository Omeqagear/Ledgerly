package com.ledgerly.payment;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentPaginationIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void shouldPaginateFindAll() {
        UUID customerId = UUID.randomUUID();
        payInvoice(customerId);
        payInvoice(customerId);
        payInvoice(customerId);

        Page<Payment> page = paymentService.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void shouldPaginateByCustomerId() {
        UUID customerId = UUID.randomUUID();
        payInvoice(customerId);
        payInvoice(customerId);

        Page<Payment> page = paymentService.findByCustomerId(customerId, PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldPaginateByInvoiceId() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        invoiceService.issueInvoice(invoice.getId());
        paymentService.processPayment(
            invoice.getId(), customerId, BigDecimal.TEN, "CARD", "TXN-" + UUID.randomUUID());

        Page<Payment> page = paymentService.findByInvoiceId(invoice.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getInvoiceId()).isEqualTo(invoice.getId());
    }

    private void payInvoice(UUID customerId) {
        Invoice invoice = invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        invoiceService.issueInvoice(invoice.getId());
        paymentService.processPayment(
            invoice.getId(), customerId, BigDecimal.TEN, "CARD", "TXN-" + UUID.randomUUID());
    }
}
