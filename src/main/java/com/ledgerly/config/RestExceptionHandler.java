package com.ledgerly.config;

import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.customer.DuplicateCustomerEmailException;
import com.ledgerly.invoice.InvoiceNotFoundException;
import com.ledgerly.payment.PaymentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handling for REST endpoints.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({
        CustomerNotFoundException.class,
        InvoiceNotFoundException.class,
        PaymentNotFoundException.class
    })
    ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateCustomerEmailException.class)
    ResponseEntity<String> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<String> handleUnprocessable(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
