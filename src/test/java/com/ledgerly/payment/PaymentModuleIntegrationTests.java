package com.ledgerly.payment;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoicePaidEvent;
import com.ledgerly.invoice.InvoiceService;
import com.ledgerly.invoice.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for the fixed payment flow: initiate payment → invoice module
 * listens for {@link PaymentProcessedEvent} → invoice marked paid →
 * {@link InvoicePaidEvent} published. Uses the full application context
 * (because the scenario spans the payment <em>and</em> invoice modules).
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class PaymentModuleIntegrationTests {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private EventCapture eventCapture;

    @Test
    void shouldProcessPaymentSuccessfully() {
        UUID customerId = UUID.randomUUID();
        Invoice invoice = issueInvoice(customerId);

        Payment payment = paymentService.processPayment(
            invoice.getId(), customerId, BigDecimal.TEN, "CARD", "TXN-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getInvoiceId()).isEqualTo(invoice.getId());

        assertThat(eventCapture.paymentEvents).anyMatch(
            e -> e.invoiceId().equals(invoice.getId()) && e.success());

        assertThat(invoiceService.findById(invoice.getId()))
            .isPresent()
            .get()
            .extracting(Invoice::getStatus)
            .isEqualTo(InvoiceStatus.PAID);

        assertThat(eventCapture.invoicePaidEvents).anyMatch(
            e -> e.invoiceId().equals(invoice.getId()));
    }

    private Invoice issueInvoice(UUID customerId) {
        Invoice invoice = invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(15));
        return invoiceService.issueInvoice(invoice.getId());
    }

    @TestConfiguration
    static class EventCaptureConfig {

        @Bean
        EventCapture eventCapture() {
            return new EventCapture();
        }
    }

    static class EventCapture {

        final List<PaymentProcessedEvent> paymentEvents = new CopyOnWriteArrayList<>();
        final List<InvoicePaidEvent> invoicePaidEvents = new CopyOnWriteArrayList<>();

        @EventListener
        void onPayment(PaymentProcessedEvent event) {
            paymentEvents.add(event);
        }

        @EventListener
        void onInvoicePaid(InvoicePaidEvent event) {
            invoicePaidEvents.add(event);
        }
    }
}