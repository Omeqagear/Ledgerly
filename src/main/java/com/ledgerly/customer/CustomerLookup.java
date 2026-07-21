package com.ledgerly.customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Narrow lookup interface for modules that only need customer contact
 * information (e.g. the notification module). Kept separate from
 * {@link CustomerAPI} as an example of interface segregation in a Modulith.
 */
public interface CustomerLookup {

    Optional<String> findEmailById(UUID customerId);

    Optional<CustomerInfo> findInfoById(UUID customerId);

    boolean exists(UUID customerId);

    /** Lightweight projection carrying only the data the notification module needs. */
    record CustomerInfo(UUID id, String email, String name, String preferredLanguage) {}
}