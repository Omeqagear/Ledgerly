package com.ledgerly.invoice.internal;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCustomerId(UUID customerId);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, java.time.LocalDate date);

    @Query("SELECT i FROM Invoice i WHERE i.status = com.ledgerly.invoice.InvoiceStatus.ISSUED AND i.dueDate < CURRENT_DATE")
    List<Invoice> findOverdueInvoices();
}