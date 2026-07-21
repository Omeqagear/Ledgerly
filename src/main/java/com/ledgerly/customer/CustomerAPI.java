package com.ledgerly.customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module contract for read-only access to customers.
 *
 * <p>Other modules should depend on this interface rather than on
 * {@link CustomerService} so Spring Modulith can verify the dependency direction.
 */
public interface CustomerAPI {

    Optional<Customer> findById(UUID id);

    boolean existsById(UUID id);
}