package com.ledgerly.reporting.internal;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class DateRangeValidatorTest {

    private final DateRangeValidator validator = new DateRangeValidator();

    @Test
    void shouldAcceptValidDateRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        
        assertThatCode(() -> validator.validate(from, to))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptNullDates() {
        assertThatCode(() -> validator.validate(null, null))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptFromDateOnly() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        
        assertThatCode(() -> validator.validate(from, null))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptToDateOnly() {
        LocalDate to = LocalDate.of(2026, 3, 31);
        
        assertThatCode(() -> validator.validate(null, to))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectFromAfterTo() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        
        assertThatThrownBy(() -> validator.validate(from, to))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("from date must be before or equal to to date");
    }
}
