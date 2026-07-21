package com.ledgerly.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Invoice aggregate root and the public type of the {@code invoice} module.
 *
 * <p>References {@link UUID customerId} only, so the invoice module does not need
 * to depend on the {@code customer} module.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, unique = true, length = 32)
    private String invoiceNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvoiceStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected Invoice() {
        // for JPA
    }

    public Invoice(UUID customerId, String invoiceNumber, BigDecimal totalAmount,
                   BigDecimal taxAmount, LocalDate dueDate) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.taxAmount = taxAmount;
        this.issueDate = LocalDate.now();
        this.dueDate = dueDate;
        this.status = InvoiceStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }

    public void issue() {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be issued (current=" + this.status + ")");
        }
        this.status = InvoiceStatus.ISSUED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPaid() {
        if (this.status != InvoiceStatus.ISSUED && this.status != InvoiceStatus.OVERDUE) {
            throw new IllegalStateException(
                "Only issued or overdue invoices can be paid (current=" + this.status + ")");
        }
        this.status = InvoiceStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsOverdue() {
        if (this.status != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can become overdue (current=" + this.status + ")");
        }
        this.status = InvoiceStatus.OVERDUE;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}