package com.ledgerly.reporting;

import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.invoice.InvoiceNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Module-scoped exception handling for reporting REST endpoints. Restricted
 * to the {@code com.ledgerly.reporting} package so it does not conflict with
 * the global {@link com.ledgerly.config.RestExceptionHandler}; given a higher
 * order so it takes precedence for reporting controllers.
 */
@RestControllerAdvice(basePackages = "com.ledgerly.reporting")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReportingExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCustomerNotFound(CustomerNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericError(Exception ex) {
        return new ErrorResponse("An unexpected error occurred");
    }

    public record ErrorResponse(String error) {}
}