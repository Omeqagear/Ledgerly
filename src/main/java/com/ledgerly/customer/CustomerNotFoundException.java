package com.ledgerly.customer;

import java.util.UUID;

/**
 * Thrown when a customer cannot be found by id.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found: " + customerId);
    }
}