package com.ledgerly.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for creating a new invoice. {@code issueDate} is deliberately
 * absent — the domain sets it to the current date when the invoice is created.
 */
public record InvoiceRequest(
    @NotNull UUID customerId,
    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal totalAmount,
    @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal taxAmount,
    @NotNull @Future LocalDate dueDate
) {}
