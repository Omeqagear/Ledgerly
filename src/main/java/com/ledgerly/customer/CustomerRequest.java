package com.ledgerly.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a customer.
 *
 * @param name               customer display name
 * @param email              customer email (unique)
 * @param taxId              tax/VAT identifier
 * @param address           postal address
 * @param preferredLanguage  ISO-639 language code (defaults to {@code en} server-side)
 */
public record CustomerRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Email @Size(max = 255) String email,
    @Size(max = 64) String taxId,
    @Size(max = 512) String address,
    @Size(max = 8) String preferredLanguage
) {}