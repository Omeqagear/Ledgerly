package com.ledgerly.reporting.internal;

import com.ledgerly.invoice.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql(statements = {
    "DROP TABLE IF EXISTS invoices",
    "CREATE TABLE invoices (id UUID, customer_id UUID, invoice_number VARCHAR, total_amount DECIMAL, tax_amount DECIMAL, issue_date DATE, due_date DATE, status VARCHAR, created_at TIMESTAMP, updated_at TIMESTAMP)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000099', 'INV-2026-000001', 100.00, 10.00, '2026-01-15', '2026-02-15', '" + InvoiceStatus.ISSUED_STR + "', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000099', 'INV-2026-000002', 200.00, 20.00, '2026-02-15', '2026-03-15', '" + InvoiceStatus.ISSUED_STR + "', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000099', 'INV-2026-000003', 300.00, 30.00, '2026-03-15', '2026-04-15', '" + InvoiceStatus.PAID_STR + "', NOW(), NULL)"
})
class ReportRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCountInvoicesByStatusWithDateRange() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 28);

        List<ReportRepository.StatusCount> counts = repository.invoiceCountsByStatus(from, to);

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).status()).isEqualTo(InvoiceStatus.ISSUED_STR);
        assertThat(counts.get(0).count()).isEqualTo(2);
    }

    @Test
    void shouldCountInvoicesByStatusWithoutDateRange() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        List<ReportRepository.StatusCount> counts = repository.invoiceCountsByStatus(null, null);

        assertThat(counts).hasSize(2);
    }

    @Test
    void shouldCountInvoicesByStatusWithFromOnly() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);
        LocalDate from = LocalDate.of(2026, 2, 1);

        List<ReportRepository.StatusCount> counts = repository.invoiceCountsByStatus(from, null);

        assertThat(counts).hasSize(2);
    }

    @Test
    void shouldCountInvoicesByStatusWithToOnly() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);
        LocalDate to = LocalDate.of(2026, 2, 28);

        List<ReportRepository.StatusCount> counts = repository.invoiceCountsByStatus(null, to);

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).status()).isEqualTo(InvoiceStatus.ISSUED_STR);
        assertThat(counts.get(0).count()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyListWhenNoInvoicesMatchDateRange() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);
        LocalDate from = LocalDate.of(2027, 1, 1);
        LocalDate to = LocalDate.of(2027, 12, 31);

        List<ReportRepository.StatusCount> counts = repository.invoiceCountsByStatus(from, to);

        assertThat(counts).isEmpty();
    }
}
