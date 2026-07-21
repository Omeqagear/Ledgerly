package com.ledgerly.customer;

/**
 * Thrown when attempting to create a customer with an email that already exists.
 */
public class DuplicateCustomerEmailException extends RuntimeException {

    public DuplicateCustomerEmailException(String email) {
        super("A customer with email '" + email + "' already exists");
    }
}