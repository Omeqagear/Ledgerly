package com.ledgerly.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for initiating a payment.
 *
 * <p>{@code customerId} and {@code amount} must be supplied by the caller
 * instead of being looked up from the invoice module. This keeps the payment
 * module free of any compile-time dependency on {@code com.ledgerly.invoice},
 * which (combined with the invoice module's inbound depend-on for the
 * {@link PaymentProcessedEvent} type) prevents cyclic module dependencies.
 */
public record PaymentRequest(
    @NotNull UUID invoiceId,
    @NotNull UUID customerId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Size(max = 32) String paymentMethod,
    @Size(max = 128) String transactionReference
) {}