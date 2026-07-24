package com.ledgerly.payment.internal;

import com.ledgerly.payment.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByInvoiceId(UUID invoiceId, Pageable pageable);

    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);
}