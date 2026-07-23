package com.ledgerly.reporting.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql(statements = {
    "DROP TABLE IF EXISTS invoices",
    "CREATE TABLE invoices (id UUID, customer_id UUID, invoice_number VARCHAR, total_amount DECIMAL, tax_amount DECIMAL, issue_date DATE, due_date DATE, status VARCHAR, created_at TIMESTAMP, updated_at TIMESTAMP)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000099', 'INV-2026-000001', 100.00, 10.00, '2026-01-15', '2026-02-15', 'ISSUED', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000099', 'INV-2026-000002', 200.00, 20.00, '2026-02-15', '2026-03-15', 'OVERDUE', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000099', 'INV-2026-000003', 300.00, 30.00, '2026-03-15', '2026-04-15', 'PAID', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000088', 'INV-2026-000004', 500.00, 50.00, '2026-04-01', '2026-05-01', 'OVERDUE', NOW(), NULL)"
})
class ReportRepositoryOutstandingInvoicesTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID CUSTOMER_99 =
        UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID CUSTOMER_88 =
        UUID.fromString("00000000-0000-0000-0000-000000000088");

    @Test
    void shouldFindOnlyIssuedAndOverdueInvoices() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        List<ReportRepository.OutstandingInvoice> outstanding = repository.findOutstandingInvoices();

        assertThat(outstanding).hasSize(3);
        assertThat(outstanding).extracting(ReportRepository.OutstandingInvoice::invoiceNumber)
            .containsExactlyInAnyOrder("INV-2026-000001", "INV-2026-000002", "INV-2026-000004");
    }

    @Test
    void shouldExcludePaidInvoices() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        List<ReportRepository.OutstandingInvoice> outstanding = repository.findOutstandingInvoices();

        assertThat(outstanding).extracting(ReportRepository.OutstandingInvoice::invoiceNumber)
            .doesNotContain("INV-2026-000003");
    }

    @Test
    void shouldMapAllOutstandingInvoiceFields() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        ReportRepository.OutstandingInvoice invoice = repository.findOutstandingInvoicesForCustomer(CUSTOMER_88)
            .get(0);

        assertThat(invoice.invoiceId()).isEqualTo(
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"));
        assertThat(invoice.customerId()).isEqualTo(CUSTOMER_88);
        assertThat(invoice.invoiceNumber()).isEqualTo("INV-2026-000004");
        assertThat(invoice.totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(invoice.dueDate()).isEqualTo(java.time.LocalDate.of(2026, 5, 1));
    }

    @Test
    void shouldFindOutstandingInvoicesForCustomer99() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        List<ReportRepository.OutstandingInvoice> outstanding =
            repository.findOutstandingInvoicesForCustomer(CUSTOMER_99);

        assertThat(outstanding).hasSize(2);
        assertThat(outstanding).extracting(ReportRepository.OutstandingInvoice::invoiceNumber)
            .containsExactlyInAnyOrder("INV-2026-000001", "INV-2026-000002");
    }

    @Test
    void shouldFindOutstandingInvoicesForCustomer88() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        List<ReportRepository.OutstandingInvoice> outstanding =
            repository.findOutstandingInvoicesForCustomer(CUSTOMER_88);

        assertThat(outstanding).hasSize(1);
        assertThat(outstanding.get(0).invoiceNumber()).isEqualTo("INV-2026-000004");
    }

    @Test
    void shouldReturnEmptyWhenCustomerHasNoOutstandingInvoices() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);

        List<ReportRepository.OutstandingInvoice> outstanding =
            repository.findOutstandingInvoicesForCustomer(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000077"));

        assertThat(outstanding).isEmpty();
    }
}