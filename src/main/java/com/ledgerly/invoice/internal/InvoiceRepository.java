package com.ledgerly.invoice.internal;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCustomerId(UUID customerId);

    @Query("SELECT i.invoiceNumber FROM Invoice i WHERE i.invoiceNumber LIKE :prefix% ORDER BY i.invoiceNumber DESC")
    Optional<String> findMaxInvoiceNumberStartingWith(String prefix);

    @Query("SELECT i FROM Invoice i WHERE i.status = com.ledgerly.invoice.InvoiceStatus.ISSUED AND i.dueDate < CURRENT_DATE")
    List<Invoice> findOverdueInvoices();

    Page<Invoice> findByCustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.status = com.ledgerly.invoice.InvoiceStatus.ISSUED AND i.dueDate < CURRENT_DATE")
    Page<Invoice> findOverdueInvoices(Pageable pageable);
}
