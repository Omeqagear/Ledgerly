package com.ledgerly.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for the {@code payment} module.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment initiate(@Valid @RequestBody PaymentRequest request) {
        return paymentService.processPayment(
            request.invoiceId(),
            request.customerId(),
            request.amount(),
            request.paymentMethod(),
            request.transactionReference()
        );
    }

    @GetMapping("/{id}")
    public Payment get(@PathVariable UUID id) {
        return paymentService.findById(id)
            .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @GetMapping
    public List<Payment> list(@RequestParam(value = "invoiceId", required = false) UUID invoiceId,
                              @RequestParam(value = "customerId", required = false) UUID customerId) {
        if (invoiceId != null) {
            return paymentService.findByInvoiceId(invoiceId);
        }
        if (customerId != null) {
            return paymentService.findByCustomerId(customerId);
        }
        return List.of();
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<String> notFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}