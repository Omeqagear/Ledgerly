package com.ledgerly.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment aggregate root and public type of the {@code payment} module.
 *
 * <p>Stores immutable {@code invoiceId} and {@code customerId} references (no
 * cross-module entity dependency); the link to the issuing invoice is purely
 * by id.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    private String paymentMethod;

    @Column(length = 128)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(length = 512)
    private String failureReason;

    private LocalDateTime processedAt;

    protected Payment() {
        // for JPA
    }

    public Payment(UUID invoiceId, UUID customerId, BigDecimal amount,
                   String paymentMethod, String transactionReference) {
        this.id = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.status = PaymentStatus.PENDING;
    }

    public void complete(LocalDateTime processedAt) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be completed (current=" + this.status + ")");
        }
        this.status = PaymentStatus.COMPLETED;
        this.processedAt = processedAt;
    }

    public void fail(String reason, LocalDateTime processedAt) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can fail (current=" + this.status + ")");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = processedAt;
    }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoiceId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionReference() { return transactionReference; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}