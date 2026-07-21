package com.ledgerly.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
class InvoiceModuleIntegrationTests {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void shouldCreateDraftInvoice() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = invoiceService.createInvoice(
            customerId, new BigDecimal("150.00"), new BigDecimal("25.50"),
            LocalDate.now().plusDays(30)
        );

        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getInvoiceNumber()).startsWith("INV-");
        assertThat(invoice.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void shouldPublishInvoiceCreatedEvent(Scenario scenario) {
        UUID customerId = UUID.randomUUID();

        scenario.stimulate(() -> invoiceService.createInvoice(
                customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(15)))
            .andWaitForEventOfType(InvoiceCreatedEvent.class)
            .matching(event -> event.customerId().equals(customerId))
            .toArriveAndVerify(event -> {
                assertThat(event.invoiceNumber()).startsWith("INV-");
                assertThat(event.totalAmount()).isEqualByComparingTo(BigDecimal.TEN);
            });
    }

    @Test
    void shouldIssueDraftInvoice() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(15));
        Invoice issued = invoiceService.issueInvoice(invoice.getId());

        assertThat(issued.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }
}