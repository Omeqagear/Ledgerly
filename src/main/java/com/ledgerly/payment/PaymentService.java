package com.ledgerly.payment;

import com.ledgerly.invoice.InvoiceAPI;
import com.ledgerly.payment.internal.PaymentGatewayClient;
import com.ledgerly.payment.internal.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public application service for {@link Payment} aggregates. Implements
 * {@link PaymentAPI} for read access from other modules.
 */
@Service
@Transactional
public class PaymentService implements PaymentAPI {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient gatewayClient;
    private final InvoiceAPI invoiceAPI;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGatewayClient gatewayClient,
                          InvoiceAPI invoiceAPI,
                          ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.gatewayClient = gatewayClient;
        this.invoiceAPI = invoiceAPI;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initiates and completes a payment. The invoice is loaded via {@link InvoiceAPI}
     * to validate state and amount before the gateway is charged. On success the
     * invoice is marked paid synchronously; a {@link PaymentProcessedEvent} is
     * still published for audit/failure notifications.
     */
    public Payment processPayment(UUID invoiceId, UUID customerId, BigDecimal amount,
                                   String paymentMethod, String transactionReference) {
        Payment payment = new Payment(invoiceId, customerId, amount, paymentMethod, transactionReference);
        payment = paymentRepository.save(payment);

        boolean success = gatewayClient.process(payment);
        LocalDateTime processedAt = LocalDateTime.now();
        PaymentProcessedEvent event;
        if (success) {
            payment.complete(processedAt);
            invoiceAPI.markAsPaid(invoiceId, amount);
            event = PaymentProcessedEvent.success(payment);
        } else {
            String reason = "Payment gateway rejected the transaction";
            payment.fail(reason, processedAt);
            event = PaymentProcessedEvent.failure(payment, reason);
        }
        payment = paymentRepository.save(payment);
        eventPublisher.publishEvent(event);
        return payment;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByInvoiceId(UUID invoiceId, Pageable pageable) {
        return paymentRepository.findByInvoiceId(invoiceId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByCustomerId(UUID customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable);
    }
}
