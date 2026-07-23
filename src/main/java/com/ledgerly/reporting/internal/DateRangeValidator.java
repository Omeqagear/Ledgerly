package com.ledgerly.reporting.internal;

import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
class DateRangeValidator {

    public void validate(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be before or equal to to date");
        }
    }
}
