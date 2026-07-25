package com.ledgerly.reporting.internal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgingCalculatorTest {

    private final AgingCalculator calculator = new AgingCalculator();

    @Test
    void shouldCategorizeDueTodayAsCurrent() {
        assertThat(calculator.categorize(LocalDate.now())).isEqualTo("Current");
    }

    @Test
    void shouldCategorizeDueTomorrowAsCurrent() {
        assertThat(calculator.categorize(LocalDate.now().plusDays(1))).isEqualTo("Current");
    }

    @Test
    void shouldCategorizeOneDayOverdueAs1To30() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(1))).isEqualTo("1-30 days");
    }

    @Test
    void shouldCategorizeThirtyDaysOverdueAs1To30() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(30))).isEqualTo("1-30 days");
    }

    @Test
    void shouldCategorizeThirtyOneDaysOverdueAs31To60() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(31))).isEqualTo("31-60 days");
    }

    @Test
    void shouldCategorizeSixtyDaysOverdueAs31To60() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(60))).isEqualTo("31-60 days");
    }

    @Test
    void shouldCategorizeSixtyOneDaysOverdueAs61To90() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(61))).isEqualTo("61-90 days");
    }

    @Test
    void shouldCategorizeNinetyDaysOverdueAs61To90() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(90))).isEqualTo("61-90 days");
    }

    @Test
    void shouldCategorizeNinetyOneDaysOverdueAs90Plus() {
        assertThat(calculator.categorize(LocalDate.now().minusDays(91))).isEqualTo("90+ days");
    }

    @Test
    void shouldCategorizeFarPastDateAs90Plus() {
        assertThat(calculator.categorize(LocalDate.now().minusYears(2))).isEqualTo("90+ days");
    }

    @Test
    void shouldGroupInvoicesByBucket() {
        LocalDate today = LocalDate.now();
        ReportRepository.OutstandingInvoice current = outstandingInvoice(today.plusDays(5), "100");
        ReportRepository.OutstandingInvoice bucket1 = outstandingInvoice(today.minusDays(10), "200");
        ReportRepository.OutstandingInvoice bucket2 = outstandingInvoice(today.minusDays(45), "300");
        ReportRepository.OutstandingInvoice bucket3 = outstandingInvoice(today.minusDays(75), "400");
        ReportRepository.OutstandingInvoice bucket4 = outstandingInvoice(today.minusDays(120), "500");

        Map<String, List<ReportRepository.OutstandingInvoice>> grouped =
            calculator.groupByBucket(List.of(current, bucket1, bucket2, bucket3, bucket4));

        assertThat(grouped).containsOnlyKeys("Current", "1-30 days", "31-60 days", "61-90 days", "90+ days");
        assertThat(grouped.get("Current")).containsExactly(current);
        assertThat(grouped.get("1-30 days")).containsExactly(bucket1);
        assertThat(grouped.get("31-60 days")).containsExactly(bucket2);
        assertThat(grouped.get("61-90 days")).containsExactly(bucket3);
        assertThat(grouped.get("90+ days")).containsExactly(bucket4);
    }

    @Test
    void shouldGroupMultipleInvoicesInSameBucket() {
        LocalDate today = LocalDate.now();
        ReportRepository.OutstandingInvoice first = outstandingInvoice(today.minusDays(5), "100");
        ReportRepository.OutstandingInvoice second = outstandingInvoice(today.minusDays(20), "200");

        Map<String, List<ReportRepository.OutstandingInvoice>> grouped =
            calculator.groupByBucket(List.of(first, second));

        assertThat(grouped.get("1-30 days")).containsExactly(first, second);
    }

    @Test
    void shouldReturnEmptyMapForEmptyList() {
        Map<String, List<ReportRepository.OutstandingInvoice>> grouped = calculator.groupByBucket(List.of());
        assertThat(grouped).isEmpty();
    }

    private static ReportRepository.OutstandingInvoice outstandingInvoice(LocalDate dueDate, String amount) {
        return new ReportRepository.OutstandingInvoice(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "INV-TEST",
            new BigDecimal(amount),
            dueDate
        );
    }
}