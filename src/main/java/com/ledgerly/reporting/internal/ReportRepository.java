package com.ledgerly.reporting.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregation queries used by the reporting module. Uses raw SQL through
 * {@link JdbcTemplate} so the reporting module has no compile-time dependency
 * on the {@code customer}, {@code invoice}, or {@code payment} modules' types
 * — only the database schema.
 */
@Component
class ReportRepository {

    private static final RowMapper<StatusCount> STATUS_COUNT_MAPPER =
        (rs, n) -> new StatusCount(rs.getString("status"), rs.getLong("cnt"));

    private final JdbcTemplate jdbc;

    ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    long countCustomers() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM customers", Long.class);
        return count != null ? count : 0L;
    }

    List<StatusCount> invoiceCountsByStatus() {
        return jdbc.query(
            "SELECT status, COUNT(*) AS cnt FROM invoices GROUP BY status",
            STATUS_COUNT_MAPPER
        );
    }

    List<StatusCount> invoiceCountsByStatus(LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT status, COUNT(*) AS cnt FROM invoices");
        List<Object> params = new ArrayList<>();

        if (from != null || to != null) {
            sql.append(" WHERE ");
            if (from != null && to != null) {
                sql.append("issue_date BETWEEN ? AND ?");
                params.add(from);
                params.add(to);
            } else if (from != null) {
                sql.append("issue_date >= ?");
                params.add(from);
            } else {
                sql.append("issue_date <= ?");
                params.add(to);
            }
        }

        sql.append(" GROUP BY status");

        return jdbc.query(
            sql.toString(),
            STATUS_COUNT_MAPPER,
            params.toArray()
        );
    }

    List<StatusCount> paymentCountsByStatus() {
        return jdbc.query(
            "SELECT status, COUNT(*) AS cnt FROM payments GROUP BY status",
            (rs, n) -> new StatusCount(rs.getString("status"), rs.getLong("cnt"))
        );
    }

    BigDecimal totalAmountPaid() {
        BigDecimal amount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'COMPLETED'",
            BigDecimal.class
        );
        return amount != null ? amount : BigDecimal.ZERO;
    }

    BigDecimal totalOutstanding() {
        BigDecimal amount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE status IN ('ISSUED', 'OVERDUE')",
            BigDecimal.class
        );
        return amount != null ? amount : BigDecimal.ZERO;
    }

    String customerName(UUID customerId) {
        return jdbc.queryForObject(
            "SELECT name FROM customers WHERE id = ?",
            String.class, customerId
        );
    }

    long countInvoicesForCustomer(UUID customerId) {
        Long cnt = jdbc.queryForObject(
            "SELECT COUNT(*) FROM invoices WHERE customer_id = ?", Long.class, customerId);
        return cnt != null ? cnt : 0L;
    }

    long countPaidInvoicesForCustomer(UUID customerId) {
        Long cnt = jdbc.queryForObject(
            "SELECT COUNT(*) FROM invoices WHERE customer_id = ? AND status = 'PAID'",
            Long.class, customerId);
        return cnt != null ? cnt : 0L;
    }

    BigDecimal totalInvoicedForCustomer(UUID customerId) {
        BigDecimal amount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE customer_id = ?",
            BigDecimal.class, customerId);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    BigDecimal totalPaidForCustomer(UUID customerId) {
        BigDecimal amount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE customer_id = ? AND status = 'COMPLETED'",
            BigDecimal.class, customerId);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    BigDecimal totalOutstandingForCustomer(UUID customerId) {
        BigDecimal amount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(total_amount), 0) FROM invoices "
                + "WHERE customer_id = ? AND status IN ('ISSUED', 'OVERDUE')",
            BigDecimal.class, customerId);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    record StatusCount(String status, long count) {}

    record OutstandingInvoice(
        UUID invoiceId,
        UUID customerId,
        String invoiceNumber,
        BigDecimal totalAmount,
        LocalDate dueDate
    ) {}

    public record CustomerInvoice(
        UUID invoiceId,
        String invoiceNumber,
        BigDecimal totalAmount,
        String status,
        LocalDate issueDate
    ) {}

    public record CustomerPayment(
        UUID paymentId,
        BigDecimal amount,
        String status,
        LocalDate processedAt
    ) {}

    List<OutstandingInvoice> findOutstandingInvoices() {
        return jdbc.query(
            "SELECT id, customer_id, invoice_number, total_amount, due_date "
                + "FROM invoices WHERE status IN ('ISSUED', 'OVERDUE')",
            (rs, n) -> new OutstandingInvoice(
                rs.getObject("id", UUID.class),
                rs.getObject("customer_id", UUID.class),
                rs.getString("invoice_number"),
                rs.getBigDecimal("total_amount"),
                rs.getDate("due_date").toLocalDate()
            )
        );
    }

    List<OutstandingInvoice> findOutstandingInvoicesForCustomer(UUID customerId) {
        return jdbc.query(
            "SELECT id, customer_id, invoice_number, total_amount, due_date "
                + "FROM invoices WHERE customer_id = ? AND status IN ('ISSUED', 'OVERDUE')",
            (rs, n) -> new OutstandingInvoice(
                rs.getObject("id", UUID.class),
                rs.getObject("customer_id", UUID.class),
                rs.getString("invoice_number"),
                rs.getBigDecimal("total_amount"),
                rs.getDate("due_date").toLocalDate()
            ),
            customerId
        );
    }

    public List<CustomerInvoice> findCustomerInvoices(UUID customerId) {
        return jdbc.query(
            "SELECT id, invoice_number, total_amount, status, issue_date "
            + "FROM invoices WHERE customer_id = ? ORDER BY issue_date DESC",
            (rs, n) -> new CustomerInvoice(
                rs.getObject("id", UUID.class),
                rs.getString("invoice_number"),
                rs.getBigDecimal("total_amount"),
                rs.getString("status"),
                rs.getDate("issue_date").toLocalDate()
            ),
            customerId
        );
    }

    public List<CustomerPayment> findCustomerPayments(UUID customerId) {
        return jdbc.query(
            "SELECT id, amount, status, processed_at "
            + "FROM payments WHERE customer_id = ? ORDER BY processed_at DESC",
            (rs, n) -> new CustomerPayment(
                rs.getObject("id", UUID.class),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                rs.getDate("processed_at") != null ? rs.getDate("processed_at").toLocalDate() : null
            ),
            customerId
        );
    }
}