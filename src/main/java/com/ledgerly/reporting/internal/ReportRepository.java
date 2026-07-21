package com.ledgerly.reporting.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
            (rs, n) -> new StatusCount(rs.getString("status"), rs.getLong("cnt"))
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
}