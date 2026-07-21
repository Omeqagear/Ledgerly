package com.ledgerly.notification.internal;

import org.springframework.stereotype.Component;

/**
 * Rudimentary message templating. A real implementation would render Thymeleaf
 * templates or similar; this scaffold just substitutes the parameters.
 */
@Component
class TemplateEngine {

    String renderInvoiceCreated(String invoiceNumber, java.math.BigDecimal total) {
        return "Your invoice " + invoiceNumber + " for " + total + " has been created.";
    }

    String renderInvoicePaid(String invoiceNumber, java.math.BigDecimal amountPaid) {
        return "Thank you! Payment of " + amountPaid + " for invoice " + invoiceNumber
            + " has been received.";
    }

    String renderPaymentFailed(String invoiceNumber, String reason) {
        return "We could not process the payment for invoice " + invoiceNumber
            + ". Reason: " + reason + ".";
    }
}