package com.ledgerly.invoice;

import com.ledgerly.invoice.internal.InvoiceNumberGenerator;
import com.ledgerly.invoice.internal.InvoiceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public application service for {@link Invoice} aggregates.
 */
@Service
@Transactional
public class InvoiceService implements InvoiceAPI {

    private final InvoiceRepository invoiceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InvoiceNumberGenerator numberGenerator;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          ApplicationEventPublisher eventPublisher,
                          InvoiceNumberGenerator numberGenerator) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
        this.numberGenerator = numberGenerator;
    }

    public Invoice createInvoice(UUID customerId, BigDecimal totalAmount,
                                  BigDecimal taxAmount, LocalDate dueDate) {
        String invoiceNumber = numberGenerator.generate();
        Invoice invoice = new Invoice(customerId, invoiceNumber, totalAmount, taxAmount, dueDate);
        invoice = invoiceRepository.save(invoice);
        eventPublisher.publishEvent(InvoiceCreatedEvent.from(invoice));
        return invoice;
    }

    public Invoice issueInvoice(UUID invoiceId) {
        Invoice invoice = load(invoiceId);
        invoice.issue();
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice markAsPaid(UUID invoiceId, BigDecimal amountPaid) {
        Invoice invoice = load(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.ISSUED && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new IllegalStateException(
                "Invoice cannot be paid (current=" + invoice.getStatus() + ")");
        }
        BigDecimal paid = amountPaid != null ? amountPaid : invoice.getTotalAmount();
        if (paid.compareTo(invoice.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                "Payment amount " + paid + " does not match invoice total " + invoice.getTotalAmount());
        }
        invoice.markAsPaid();
        invoice = invoiceRepository.save(invoice);
        eventPublisher.publishEvent(new InvoicePaidEvent(
            invoice.getId(), invoice.getCustomerId(), paid, LocalDateTime.now()));
        return invoice;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> findByCustomerId(UUID customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> findOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices();
    }

    @Transactional(readOnly = true)
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    private Invoice load(UUID id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new InvoiceNotFoundException(id));
    }
}
