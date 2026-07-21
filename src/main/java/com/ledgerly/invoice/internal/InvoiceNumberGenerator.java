package com.ledgerly.invoice.internal;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates sequential, year-stamped invoice numbers of the form
 * {@code INV-2024-000001}.
 *
 * <p>The counter is seeded from the highest existing invoice number for the
 * current year on startup, so numbers are not reused across restarts.
 */
@Component
public class InvoiceNumberGenerator {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^INV-(\\d{4})-(\\d+)$");

    private final AtomicInteger counter = new AtomicInteger(0);
    private final InvoiceRepository invoiceRepository;

    public InvoiceNumberGenerator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @PostConstruct
    void seedCounter() {
        String prefix = "INV-" + Year.now() + "-";
        Optional<Integer> maxSequence = invoiceRepository.findMaxInvoiceNumberStartingWith(prefix)
            .flatMap(this::extractSequence);
        maxSequence.ifPresent(counter::set);
    }

    public String generate() {
        return "INV-" + Year.now() + "-" + String.format("%06d", counter.incrementAndGet());
    }

    private Optional<Integer> extractSequence(String invoiceNumber) {
        Matcher matcher = NUMBER_PATTERN.matcher(invoiceNumber);
        if (matcher.matches()) {
            try {
                return Optional.of(Integer.parseInt(matcher.group(2)));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
