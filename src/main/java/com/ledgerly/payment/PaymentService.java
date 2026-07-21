package com.ledgerly.payment;

import com.ledgerly.payment.internal.PaymentGatewayClient;
import com.ledgerly.payment.internal.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public application service for {@link Payment} aggregates. Implements
 * {@link PaymentAPI} for read access from other modules.
 */
@Service
@Transactional
public class PaymentService implements PaymentAPI {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient gatewayClient;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGatewayClient gatewayClient,
                          ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.gatewayClient = gatewayClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Initiates and completes a payment in a single request by calling the
     * gateway synchronously. Publishes {@link PaymentProcessedEvent}.
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

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByInvoiceId(UUID invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByCustomerId(UUID customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }
}