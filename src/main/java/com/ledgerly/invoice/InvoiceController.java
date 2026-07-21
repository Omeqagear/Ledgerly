package com.ledgerly.invoice;

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
 * REST controller for the {@code invoice} module.
 *
 * <p>Note: there is no {@code POST /invoices/{id}/pay} endpoint on purpose —
 * payments are initiated through the {@code payment} module.
 */
@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice create(@Valid @RequestBody InvoiceRequest request) {
        return invoiceService.createInvoice(
            request.customerId(),
            request.totalAmount(),
            request.taxAmount(),
            request.dueDate()
        );
    }

    @GetMapping
    public List<Invoice> list(@RequestParam(value = "customerId", required = false) UUID customerId,
                              @RequestParam(value = "overdue", required = false, defaultValue = "false") boolean overdue) {
        if (overdue) {
            return invoiceService.findOverdueInvoices();
        }
        if (customerId != null) {
            return invoiceService.findByCustomerId(customerId);
        }
        return invoiceService.findAll();
    }

    @GetMapping("/{id}")
    public Invoice get(@PathVariable UUID id) {
        return invoiceService.findById(id)
            .orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    @PostMapping("/{id}/issue")
    public Invoice issue(@PathVariable UUID id) {
        return invoiceService.issueInvoice(id);
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    ResponseEntity<String> notFound(InvoiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}