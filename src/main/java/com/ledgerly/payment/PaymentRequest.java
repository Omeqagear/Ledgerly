package com.ledgerly.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for initiating a payment.
 */
public record PaymentRequest(
    @NotNull UUID invoiceId,
    @NotNull UUID customerId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @NotBlank @Size(max = 32) String paymentMethod,
    @Size(max = 128) String transactionReference
) {}
