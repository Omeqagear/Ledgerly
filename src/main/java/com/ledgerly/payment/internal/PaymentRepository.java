package com.ledgerly.payment.internal;

import com.ledgerly.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByCustomerId(UUID customerId);
}