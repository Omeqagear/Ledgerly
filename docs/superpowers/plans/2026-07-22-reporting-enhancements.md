# Phase 6: Reporting Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the reporting module with PDF/Excel generation, date-range filtering, and aging reports.

**Architecture:** On-demand document generation in the reporting module using OpenPDF (PDF) and Apache POI (Excel). The reporting module gains dependencies on invoice and customer modules for data access. Separate endpoints for JSON, PDF, and Excel formats.

**Tech Stack:** Spring Boot 3.2.5, OpenPDF 2.0.3, Apache POI 5.2.5, Spring Modulith 1.3.0

---

## File Structure

**New files in `com.ledgerly.reporting`:**
- `AgingReport.java` - Aging report record
- `AgingBucket.java` - Individual aging bucket record
- `internal/AgingCalculator.java` - Bucket assignment logic
- `internal/DateRangeValidator.java` - Date parameter validation
- `internal/PdfService.java` - PDF generation orchestration
- `internal/PdfRenderer.java` - OpenPDF rendering
- `internal/ExcelService.java` - Excel generation orchestration
- `internal/ExcelRenderer.java` - Apache POI rendering
- `internal/InvoiceDataProvider.java` - Fetches invoice/customer data for PDFs
- `ReportingExceptionHandler.java` - Error handling for reporting endpoints

**Modified files:**
- `ReportService.java` - Add date-range and aging methods
- `ReportController.java` - Add new endpoints
- `ReportGenerator.java` - Add aging calculation
- `ReportRepository.java` - Add date-filtered and outstanding invoice queries
- `package-info.java` - Add allowedDependencies

**Test files:**
- `internal/AgingCalculatorTest.java`
- `internal/DateRangeValidatorTest.java`
- `internal/PdfRendererTest.java`
- `internal/ExcelRendererTest.java`
- `ReportControllerIntegrationTest.java` - Extend with new endpoint tests

---

## Phase 1: Date-Range Filtering

### Task 1: DateRangeValidator

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/DateRangeValidator.java`
- Create: `src/test/java/com/ledgerly/reporting/internal/DateRangeValidatorTest.java`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=DateRangeValidatorTest`
Expected: FAIL with "DateRangeValidator cannot be resolved"

- [ ] **Step 3: Write minimal implementation**

```java
package com.ledgerly.reporting.internal;

import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DateRangeValidator {

    public void validate(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be before or equal to to date");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=DateRangeValidatorTest`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/DateRangeValidator.java src/test/java/com/ledgerly/reporting/internal/DateRangeValidatorTest.java
git commit -m "feat(reporting): add DateRangeValidator with tests"
```

---

### Task 2: Extend ReportRepository with Date-Filtered Methods

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ReportRepository.java:30-42`
- Create: `src/test/java/com/ledgerly/reporting/internal/ReportRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ledgerly.reporting.internal;

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
    "CREATE TABLE invoices (id UUID, customer_id UUID, invoice_number VARCHAR, total_amount DECIMAL, tax_amount DECIMAL, issue_date DATE, due_date DATE, status VARCHAR, created_at TIMESTAMP, updated_at TIMESTAMP)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000099', 'INV-2026-000001', 100.00, 10.00, '2026-01-15', '2026-02-15', 'ISSUED', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000099', 'INV-2026-000002', 200.00, 20.00, '2026-02-15', '2026-03-15', 'ISSUED', NOW(), NULL)",
    "INSERT INTO invoices VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000099', 'INV-2026-000003', 300.00, 30.00, '2026-03-15', '2026-04-15', 'PAID', NOW(), NULL)"
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
        assertThat(counts.get(0).status()).isEqualTo("ISSUED");
        assertThat(counts.get(0).count()).isEqualTo(2);
    }

    @Test
    void shouldCountInvoicesByStatusWithoutDateRange() {
        ReportRepository repository = new ReportRepository(jdbcTemplate);
        
        List<ReportRepository.StatusCount> counts = repository.invoiceCountsByStatus(null, null);
        
        assertThat(counts).hasSize(2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=ReportRepositoryTest`
Expected: FAIL with "method invoiceCountsByStatus(LocalDate, LocalDate) is undefined"

- [ ] **Step 3: Write minimal implementation**

```java
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
        (rs, n) -> new StatusCount(rs.getString("status"), rs.getLong("cnt")),
        params.toArray()
    );
}
```

Add the import at the top of ReportRepository.java:
```java
import java.time.LocalDate;
import java.util.ArrayList;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=ReportRepositoryTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ReportRepository.java src/test/java/com/ledgerly/reporting/internal/ReportRepositoryTest.java
git commit -m "feat(reporting): add date-filtered invoice counting to ReportRepository"
```

---

### Task 3: Update ReportService with Date Parameters

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java:22-24`

- [ ] **Step 1: Modify ReportService**

```java
public OverallSummary overallSummary() {
    return overallSummary(null, null);
}

public OverallSummary overallSummary(LocalDate from, LocalDate to) {
    dateRangeValidator.validate(from, to);
    return reportGenerator.overall(from, to);
}
```

Add the field and constructor parameter:
```java
private final com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator;

public ReportService(com.ledgerly.reporting.internal.ReportGenerator reportGenerator,
                     com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator) {
    this.reportGenerator = reportGenerator;
    this.dateRangeValidator = dateRangeValidator;
}
```

Add import:
```java
import java.time.LocalDate;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportService.java
git commit -m "feat(reporting): add date parameters to ReportService.overallSummary()"
```

---

### Task 4: Update ReportGenerator with Date Parameters

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ReportGenerator.java:24-38`

- [ ] **Step 1: Modify ReportGenerator**

```java
public OverallSummary overall() {
    return overall(null, null);
}

public OverallSummary overall(LocalDate from, LocalDate to) {
    long totalCustomers = reportRepository.countCustomers();
    
    Map<String, Long> invoicesByStatus = toStatusMap(reportRepository.invoiceCountsByStatus(from, to));
    Map<String, Long> paymentsByStatus = toStatusMap(reportRepository.paymentCountsByStatus());
    
    BigDecimal totalAmountPaid = reportRepository.totalAmountPaid();
    BigDecimal totalOutstanding = reportRepository.totalOutstanding();
    
    return new OverallSummary(totalCustomers, invoicesByStatus, paymentsByStatus, totalAmountPaid, totalOutstanding);
}
```

Add import:
```java
import java.time.LocalDate;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ReportGenerator.java
git commit -m "feat(reporting): add date parameters to ReportGenerator.overall()"
```

---

### Task 5: Update ReportController with Date Parameters

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java:23-26`

- [ ] **Step 1: Modify ReportController**

```java
@GetMapping("/summary")
public OverallSummary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return reportService.overallSummary(from, to);
}
```

Add imports:
```java
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportController.java
git commit -m "feat(reporting): add date query parameters to /reports/summary endpoint"
```

---

## Phase 2: Aging Reports

### Task 6: Create AgingBucket and AgingReport Records

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/AgingBucket.java`
- Create: `src/main/java/com/ledgerly/reporting/AgingReport.java`

- [ ] **Step 1: Create AgingBucket record**

```java
package com.ledgerly.reporting;

import java.math.BigDecimal;

public record AgingBucket(
    String name,
    long invoiceCount,
    BigDecimal totalAmount
) {}
```

- [ ] **Step 2: Create AgingReport record**

```java
package com.ledgerly.reporting;

import java.math.BigDecimal;
import java.util.List;

public record AgingReport(
    List<AgingBucket> buckets,
    BigDecimal totalOutstanding
) {}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/AgingBucket.java src/main/java/com/ledgerly/reporting/AgingReport.java
git commit -m "feat(reporting): add AgingBucket and AgingReport records"
```

---

### Task 7: Create AgingCalculator

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/AgingCalculator.java`
- Create: `src/test/java/com/ledgerly/reporting/internal/AgingCalculatorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ledgerly.reporting.internal;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgingCalculatorTest {

    private final AgingCalculator calculator = new AgingCalculator();

    @Test
    void shouldCategorizeCurrentInvoice() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(5);
        
        String bucket = calculator.categorize(dueDate);
        
        assertThat(bucket).isEqualTo("Current");
    }

    @Test
    void shouldCategorizeDueTodayAsCurrent() {
        LocalDate today = LocalDate.now();
        
        String bucket = calculator.categorize(today);
        
        assertThat(bucket).isEqualTo("Current");
    }

    @Test
    void shouldCategorize1To30DaysOverdue() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.minusDays(15);
        
        String bucket = calculator.categorize(dueDate);
        
        assertThat(bucket).isEqualTo("1-30 days");
    }

    @Test
    void shouldCategorize31To60DaysOverdue() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.minusDays(45);
        
        String bucket = calculator.categorize(dueDate);
        
        assertThat(bucket).isEqualTo("31-60 days");
    }

    @Test
    void shouldCategorize61To90DaysOverdue() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.minusDays(75);
        
        String bucket = calculator.categorize(dueDate);
        
        assertThat(bucket).isEqualTo("61-90 days");
    }

    @Test
    void shouldCategorize90PlusDaysOverdue() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.minusDays(100);
        
        String bucket = calculator.categorize(dueDate);
        
        assertThat(bucket).isEqualTo("90+ days");
    }

    @Test
    void shouldGroupInvoicesByBucket() {
        LocalDate today = LocalDate.now();
        
        List<ReportRepository.OutstandingInvoice> invoices = List.of(
            new ReportRepository.OutstandingInvoice(null, null, "INV-001", new java.math.BigDecimal("100.00"), today.plusDays(5)),
            new ReportRepository.OutstandingInvoice(null, null, "INV-002", new java.math.BigDecimal("200.00"), today.minusDays(10)),
            new ReportRepository.OutstandingInvoice(null, null, "INV-003", new java.math.BigDecimal("300.00"), today.minusDays(40))
        );
        
        Map<String, List<ReportRepository.OutstandingInvoice>> grouped = calculator.groupByBucket(invoices);
        
        assertThat(grouped.get("Current")).hasSize(1);
        assertThat(grouped.get("1-30 days")).hasSize(1);
        assertThat(grouped.get("31-60 days")).hasSize(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=AgingCalculatorTest`
Expected: FAIL with "AgingCalculator cannot be resolved"

- [ ] **Step 3: Write minimal implementation**

```java
package com.ledgerly.reporting.internal;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AgingCalculator {

    public String categorize(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
        
        if (daysOverdue <= 0) {
            return "Current";
        } else if (daysOverdue <= 30) {
            return "1-30 days";
        } else if (daysOverdue <= 60) {
            return "31-60 days";
        } else if (daysOverdue <= 90) {
            return "61-90 days";
        } else {
            return "90+ days";
        }
    }

    public Map<String, List<ReportRepository.OutstandingInvoice>> groupByBucket(
            List<ReportRepository.OutstandingInvoice> invoices) {
        
        Map<String, List<ReportRepository.OutstandingInvoice>> grouped = new HashMap<>();
        
        for (ReportRepository.OutstandingInvoice invoice : invoices) {
            String bucket = categorize(invoice.dueDate());
            grouped.computeIfAbsent(bucket, k -> new java.util.ArrayList<>()).add(invoice);
        }
        
        return grouped;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=AgingCalculatorTest`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/AgingCalculator.java src/test/java/com/ledgerly/reporting/internal/AgingCalculatorTest.java
git commit -m "feat(reporting): add AgingCalculator with bucket assignment logic"
```

---

### Task 8: Extend ReportRepository for Outstanding Invoices

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ReportRepository.java:102-103`
- Modify: `src/test/java/com/ledgerly/reporting/internal/ReportRepositoryTest.java`

- [ ] **Step 1: Add OutstandingInvoice record to ReportRepository**

Add at the end of ReportRepository class:
```java
public record OutstandingInvoice(
    java.util.UUID invoiceId,
    java.util.UUID customerId,
    String invoiceNumber,
    java.math.BigDecimal totalAmount,
    java.time.LocalDate dueDate
) {}
```

- [ ] **Step 2: Add query methods**

```java
public List<OutstandingInvoice> findOutstandingInvoices() {
    return jdbc.query(
        "SELECT id, customer_id, invoice_number, total_amount, due_date " +
        "FROM invoices WHERE status IN ('ISSUED', 'OVERDUE')",
        (rs, n) -> new OutstandingInvoice(
            rs.getObject("id", java.util.UUID.class),
            rs.getObject("customer_id", java.util.UUID.class),
            rs.getString("invoice_number"),
            rs.getBigDecimal("total_amount"),
            rs.getDate("due_date").toLocalDate()
        )
    );
}

public List<OutstandingInvoice> findOutstandingInvoicesForCustomer(java.util.UUID customerId) {
    return jdbc.query(
        "SELECT id, customer_id, invoice_number, total_amount, due_date " +
        "FROM invoices WHERE customer_id = ? AND status IN ('ISSUED', 'OVERDUE')",
        (rs, n) -> new OutstandingInvoice(
            rs.getObject("id", java.util.UUID.class),
            rs.getObject("customer_id", java.util.UUID.class),
            rs.getString("invoice_number"),
            rs.getBigDecimal("total_amount"),
            rs.getDate("due_date").toLocalDate()
        ),
        customerId
    );
}
```

- [ ] **Step 3: Add test**

```java
@Test
void shouldFindOutstandingInvoices() {
    ReportRepository repository = new ReportRepository(jdbcTemplate);
    
    List<ReportRepository.OutstandingInvoice> outstanding = repository.findOutstandingInvoices();
    
    assertThat(outstanding).hasSize(2);
    assertThat(outstanding.get(0).invoiceNumber()).isIn("INV-2026-000001", "INV-2026-000002");
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=ReportRepositoryTest`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ReportRepository.java src/test/java/com/ledgerly/reporting/internal/ReportRepositoryTest.java
git commit -m "feat(reporting): add outstanding invoice queries to ReportRepository"
```

---

### Task 9: Extend ReportGenerator with Aging Calculation

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ReportGenerator.java`

- [ ] **Step 1: Add aging calculation method**

```java
public AgingReport agingReport() {
    List<ReportRepository.OutstandingInvoice> outstanding = reportRepository.findOutstandingInvoices();
    
    Map<String, List<ReportRepository.OutstandingInvoice>> grouped = agingCalculator.groupByBucket(outstanding);
    
    List<AgingBucket> buckets = createBuckets(grouped);
    BigDecimal totalOutstanding = calculateTotalOutstanding(outstanding);
    
    return new AgingReport(buckets, totalOutstanding);
}

public AgingReport agingReportForCustomer(java.util.UUID customerId) {
    List<ReportRepository.OutstandingInvoice> outstanding = 
        reportRepository.findOutstandingInvoicesForCustomer(customerId);
    
    Map<String, List<ReportRepository.OutstandingInvoice>> grouped = agingCalculator.groupByBucket(outstanding);
    
    List<AgingBucket> buckets = createBuckets(grouped);
    BigDecimal totalOutstanding = calculateTotalOutstanding(outstanding);
    
    return new AgingReport(buckets, totalOutstanding);
}

private List<AgingBucket> createBuckets(Map<String, List<ReportRepository.OutstandingInvoice>> grouped) {
    List<String> bucketOrder = List.of("Current", "1-30 days", "31-60 days", "61-90 days", "90+ days");
    
    return bucketOrder.stream()
        .map(bucketName -> {
            List<ReportRepository.OutstandingInvoice> invoices = grouped.getOrDefault(bucketName, List.of());
            long count = invoices.size();
            BigDecimal total = invoices.stream()
                .map(ReportRepository.OutstandingInvoice::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AgingBucket(bucketName, count, total);
        })
        .toList();
}

private BigDecimal calculateTotalOutstanding(List<ReportRepository.OutstandingInvoice> outstanding) {
    return outstanding.stream()
        .map(ReportRepository.OutstandingInvoice::totalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

- [ ] **Step 2: Add AgingCalculator to constructor**

```java
private final AgingCalculator agingCalculator;

public ReportGenerator(ReportRepository reportRepository, AgingCalculator agingCalculator) {
    this.reportRepository = reportRepository;
    this.agingCalculator = agingCalculator;
}
```

- [ ] **Step 3: Add imports**

```java
import com.ledgerly.reporting.AgingBucket;
import com.ledgerly.reporting.AgingReport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ReportGenerator.java
git commit -m "feat(reporting): add aging calculation to ReportGenerator"
```

---

### Task 10: Add Aging Endpoints to ReportService and ReportController

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java`

- [ ] **Step 1: Add methods to ReportService**

```java
public AgingReport agingReport() {
    return reportGenerator.agingReport();
}

public AgingReport agingReportForCustomer(java.util.UUID customerId) {
    return reportGenerator.agingReportForCustomer(customerId);
}
```

Add import:
```java
import com.ledgerly.reporting.AgingReport;
```

- [ ] **Step 2: Add endpoints to ReportController**

```java
@GetMapping("/aging")
public AgingReport aging() {
    return reportService.agingReport();
}

@GetMapping("/customers/{customerId}/aging")
public AgingReport customerAging(@PathVariable java.util.UUID customerId) {
    return reportService.agingReportForCustomer(customerId);
}
```

Add import:
```java
import com.ledgerly.reporting.AgingReport;
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportService.java src/main/java/com/ledgerly/reporting/ReportController.java
git commit -m "feat(reporting): add aging report endpoints"
```

---

## Phase 3: PDF Generation

### Task 11: Add OpenPDF Dependency

**Files:**
- Modify: `pom.xml:28-50`

- [ ] **Step 1: Add OpenPDF dependency to pom.xml**

Add after the Spring Modulith dependencies section:
```xml
<!-- PDF Generation -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>2.0.3</version>
</dependency>
```

- [ ] **Step 2: Verify dependency resolves**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B dependency:resolve`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add OpenPDF 2.0.3 dependency for PDF generation"
```

---

### Task 12: Create InvoiceDataProvider

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/InvoiceDataProvider.java`
- Create: `src/test/java/com/ledgerly/reporting/internal/InvoiceDataProviderTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ledgerly.reporting.internal;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceDataProviderTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private InvoiceDataProvider provider;

    @Test
    void shouldFetchInvoiceWithCustomerDetails() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        
        Invoice invoice = createInvoice(invoiceId, customerId);
        Customer customer = createCustomer(customerId);
        
        when(invoiceService.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerService.findById(customerId)).thenReturn(Optional.of(customer));
        
        InvoiceDataProvider.InvoiceWithCustomer result = provider.fetchInvoiceWithCustomer(invoiceId);
        
        assertThat(result.invoice()).isEqualTo(invoice);
        assertThat(result.customer()).isEqualTo(customer);
    }

    private Invoice createInvoice(UUID id, UUID customerId) {
        return new Invoice(customerId, "INV-2026-000001", 
            new BigDecimal("100.00"), new BigDecimal("10.00"), 
            LocalDate.of(2026, 2, 15)) {
            @Override
            public UUID getId() { return id; }
            @Override
            public UUID getCustomerId() { return customerId; }
            @Override
            public String getInvoiceNumber() { return "INV-2026-000001"; }
            @Override
            public BigDecimal getTotalAmount() { return new BigDecimal("100.00"); }
            @Override
            public BigDecimal getTaxAmount() { return new BigDecimal("10.00"); }
            @Override
            public LocalDate getIssueDate() { return LocalDate.of(2026, 1, 15); }
            @Override
            public LocalDate getDueDate() { return LocalDate.of(2026, 2, 15); }
            @Override
            public com.ledgerly.invoice.InvoiceStatus getStatus() { 
                return com.ledgerly.invoice.InvoiceStatus.ISSUED; 
            }
        };
    }

    private Customer createCustomer(UUID id) {
        return new Customer("Acme Corp", "acme@example.com", "TAX123", "123 Main St") {
            @Override
            public UUID getId() { return id; }
        };
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=InvoiceDataProviderTest`
Expected: FAIL with "InvoiceDataProvider cannot be resolved"

- [ ] **Step 3: Write minimal implementation**

```java
package com.ledgerly.reporting.internal;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceNotFoundException;
import com.ledgerly.invoice.InvoiceService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InvoiceDataProvider {

    private final InvoiceService invoiceService;
    private final CustomerService customerService;

    public InvoiceDataProvider(InvoiceService invoiceService, CustomerService customerService) {
        this.invoiceService = invoiceService;
        this.customerService = customerService;
    }

    public InvoiceWithCustomer fetchInvoiceWithCustomer(UUID invoiceId) {
        Invoice invoice = invoiceService.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        
        Customer customer = customerService.findById(invoice.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(invoice.getCustomerId()));
        
        return new InvoiceWithCustomer(invoice, customer);
    }

    public record InvoiceWithCustomer(Invoice invoice, Customer customer) {}
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=InvoiceDataProviderTest`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/InvoiceDataProvider.java src/test/java/com/ledgerly/reporting/internal/InvoiceDataProviderTest.java
git commit -m "feat(reporting): add InvoiceDataProvider for fetching invoice with customer details"
```

---

### Task 13: Create PdfRenderer for Invoice PDFs

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/PdfRenderer.java`
- Create: `src/test/java/com/ledgerly/reporting/internal/PdfRendererTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ledgerly.reporting.internal;

import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfRendererTest {

    private final PdfRenderer renderer = new PdfRenderer();

    @Test
    void shouldRenderValidPdf() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        renderer.renderInvoicePdf(output, "Test Customer", "INV-2026-000001", 
            "2026-01-15", "2026-02-15", "100.00", "10.00", "110.00", "ISSUED");
        
        byte[] pdfBytes = output.toByteArray();
        assertThat(pdfBytes).isNotEmpty();
        
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        assertThat(reader.getNumberOfPages()).isGreaterThan(0);
        reader.close();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=PdfRendererTest`
Expected: FAIL with "PdfRenderer cannot be resolved"

- [ ] **Step 3: Write minimal implementation**

```java
package com.ledgerly.reporting.internal;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDate;

@Component
public class PdfRenderer {

    private static final Color PRIMARY_COLOR = new Color(44, 62, 80);
    private static final Color ACCENT_COLOR = new Color(127, 140, 141);
    private static final Color HEADER_BG = new Color(214, 234, 248);
    private static final Color ROW_ALT = new Color(242, 243, 244);

    public void renderInvoicePdf(OutputStream output, String customerName, String invoiceNumber,
                                  String issueDate, String dueDate, String subtotal,
                                  String tax, String total, String status) throws DocumentException {
        
        Document document = new Document(PageSize.A4, 56.7f, 56.7f, 56.7f, 56.7f);
        PdfWriter.getInstance(document, output);
        document.open();

        renderHeader(document, invoiceNumber, issueDate, dueDate);
        renderCustomerSection(document, customerName);
        renderLineItemsTable(document, subtotal, tax, total);
        renderTotalsSection(document, subtotal, tax, total, status);

        document.close();
    }

    private void renderHeader(Document document, String invoiceNumber, String issueDate, String dueDate) 
            throws DocumentException {
        
        Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
        Paragraph title = new Paragraph("Ledgerly", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        Font subtitleFont = new Font(Font.HELVETICA, 16, Font.BOLD, ACCENT_COLOR);
        Paragraph subtitle = new Paragraph("INVOICE", subtitleFont);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 1});

        addHeaderCell(headerTable, "Invoice Number:", invoiceNumber, Element.ALIGN_RIGHT);
        addHeaderCell(headerTable, "Issue Date:", issueDate, Element.ALIGN_RIGHT);
        addHeaderCell(headerTable, "", "");
        addHeaderCell(headerTable, "Due Date:", dueDate, Element.ALIGN_RIGHT);

        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void renderCustomerSection(Document document, String customerName) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
        Paragraph section = new Paragraph("Bill To:", sectionFont);
        section.setSpacingAfter(5);
        document.add(section);

        Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
        Paragraph customer = new Paragraph(customerName, bodyFont);
        customer.setSpacingAfter(20);
        document.add(customer);
    }

    private void renderLineItemsTable(Document document, String subtotal, String tax, String total) 
            throws DocumentException {
        
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1, 1});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        
        addTableHeader(table, "Description", headerFont);
        addTableHeader(table, "Quantity", headerFont);
        addTableHeader(table, "Unit Price", headerFont);
        addTableHeader(table, "Amount", headerFont);

        Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        
        addTableRow(table, "Invoice Total", "1", subtotal, subtotal, bodyFont, false);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void renderTotalsSection(Document document, String subtotal, String tax, String total, String status) 
            throws DocumentException {
        
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setWidths(new float[]{1, 1});

        Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

        addTotalRow(totalsTable, "Subtotal:", subtotal, labelFont, valueFont);
        addTotalRow(totalsTable, "Tax:", tax, labelFont, valueFont);
        
        Font totalLabelFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
        Font totalValueFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
        addTotalRow(totalsTable, "Total:", total, totalLabelFont, totalValueFont);

        document.add(totalsTable);
        document.add(new Paragraph(" "));

        Font statusFont = new Font(Font.HELVETICA, 12, Font.BOLD, getStatusColor(status));
        Paragraph statusPara = new Paragraph("Status: " + status, statusFont);
        statusPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(statusPara);
    }

    private void addHeaderCell(PdfPTable table, String label, String value, int align) {
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.BLACK);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setHorizontalAlignment(align);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setHorizontalAlignment(align);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String desc, String qty, String price, String amount, 
                             Font font, boolean altRow) {
        
        Color bgColor = altRow ? ROW_ALT : Color.WHITE;

        PdfPCell descCell = new PdfPCell(new Phrase(desc, font));
        descCell.setBackgroundColor(bgColor);
        descCell.setPadding(8);
        table.addCell(descCell);

        PdfPCell qtyCell = new PdfPCell(new Phrase(qty, font));
        qtyCell.setBackgroundColor(bgColor);
        qtyCell.setPadding(8);
        qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(qtyCell);

        PdfPCell priceCell = new PdfPCell(new Phrase(price, font));
        priceCell.setBackgroundColor(bgColor);
        priceCell.setPadding(8);
        priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(priceCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(amount, font));
        amountCell.setBackgroundColor(bgColor);
        amountCell.setPadding(8);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private Color getStatusColor(String status) {
        return switch (status) {
            case "PAID" -> new Color(39, 174, 96);
            case "OVERDUE" -> new Color(231, 76, 60);
            case "ISSUED" -> new Color(52, 152, 219);
            default -> ACCENT_COLOR;
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=PdfRendererTest`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/PdfRenderer.java src/test/java/com/ledgerly/reporting/internal/PdfRendererTest.java
git commit -m "feat(reporting): add PdfRenderer for invoice PDF generation"
```

---

### Task 14: Create PdfService

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/PdfService.java`

- [ ] **Step 1: Write PdfService implementation**

```java
package com.ledgerly.reporting.internal;

import com.lowagie.text.DocumentException;
import com.ledgerly.customer.Customer;
import com.ledgerly.invoice.Invoice;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PdfService {

    private final InvoiceDataProvider invoiceDataProvider;
    private final PdfRenderer pdfRenderer;

    public PdfService(InvoiceDataProvider invoiceDataProvider, PdfRenderer pdfRenderer) {
        this.invoiceDataProvider = invoiceDataProvider;
        this.pdfRenderer = pdfRenderer;
    }

    public void generateInvoicePdf(OutputStream output, UUID invoiceId) {
        InvoiceDataProvider.InvoiceWithCustomer data = invoiceDataProvider.fetchInvoiceWithCustomer(invoiceId);
        
        Invoice invoice = data.invoice();
        Customer customer = data.customer();

        try {
            pdfRenderer.renderInvoicePdf(
                output,
                customer.getName(),
                invoice.getInvoiceNumber(),
                formatDate(invoice.getIssueDate()),
                formatDate(invoice.getDueDate()),
                formatAmount(invoice.getTotalAmount()),
                formatAmount(invoice.getTaxAmount()),
                formatAmount(invoice.getTotalAmount()),
                invoice.getStatus().name()
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        return amount.toPlainString();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/PdfService.java
git commit -m "feat(reporting): add PdfService for orchestrating PDF generation"
```

---

### Task 15: Add Invoice PDF Endpoint

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java`

- [ ] **Step 1: Add invoice PDF endpoint**

```java
@GetMapping("/invoices/{invoiceId}/pdf")
public void invoicePdf(@PathVariable java.util.UUID invoiceId, HttpServletResponse response) throws IOException {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment; filename=\"invoice-" + invoiceId + ".pdf\"");
    
    reportService.generateInvoicePdf(response.getOutputStream(), invoiceId);
}
```

Add imports:
```java
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
```

- [ ] **Step 2: Add method to ReportService**

```java
public void generateInvoicePdf(java.io.OutputStream output, java.util.UUID invoiceId) {
    pdfService.generateInvoicePdf(output, invoiceId);
}
```

Add field and constructor parameter:
```java
private final com.ledgerly.reporting.internal.PdfService pdfService;

public ReportService(com.ledgerly.reporting.internal.ReportGenerator reportGenerator,
                     com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator,
                     com.ledgerly.reporting.internal.PdfService pdfService) {
    this.reportGenerator = reportGenerator;
    this.dateRangeValidator = dateRangeValidator;
    this.pdfService = pdfService;
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportController.java src/main/java/com/ledgerly/reporting/ReportService.java
git commit -m "feat(reporting): add /reports/invoices/{id}/pdf endpoint"
```

---

### Task 16: Extend PdfRenderer for Customer Statements

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/PdfRenderer.java`

- [ ] **Step 1: Add customer statement rendering method**

```java
public void renderCustomerStatementPdf(OutputStream output, String customerName, String statementPeriod,
                                        String totalInvoiced, String totalPaid, String outstanding,
                                        List<TransactionRow> transactions) throws DocumentException {
    
    Document document = new Document(PageSize.A4, 56.7f, 56.7f, 56.7f, 56.7f);
    PdfWriter.getInstance(document, output);
    document.open();

    renderStatementHeader(document, customerName, statementPeriod);
    renderStatementSummary(document, totalInvoiced, totalPaid, outstanding);
    renderTransactionTable(document, transactions);
    renderFooter(document);

    document.close();
}

private void renderStatementHeader(Document document, String customerName, String statementPeriod) 
        throws DocumentException {
    
    Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
    Paragraph title = new Paragraph("Ledgerly", titleFont);
    title.setAlignment(Element.ALIGN_LEFT);
    document.add(title);

    Font subtitleFont = new Font(Font.HELVETICA, 16, Font.BOLD, ACCENT_COLOR);
    Paragraph subtitle = new Paragraph("CUSTOMER STATEMENT", subtitleFont);
    subtitle.setSpacingAfter(20);
    document.add(subtitle);

    Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
    Paragraph customer = new Paragraph("Customer: " + customerName, bodyFont);
    customer.setSpacingAfter(5);
    document.add(customer);

    Paragraph period = new Paragraph("Statement Period: " + statementPeriod, bodyFont);
    period.setSpacingAfter(20);
    document.add(period);
}

private void renderStatementSummary(Document document, String totalInvoiced, String totalPaid, String outstanding) 
        throws DocumentException {
    
    Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
    Paragraph section = new Paragraph("Summary", sectionFont);
    section.setSpacingAfter(10);
    document.add(section);

    PdfPTable summaryTable = new PdfPTable(2);
    summaryTable.setWidthPercentage(60);
    summaryTable.setWidths(new float[]{1, 1});

    Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
    Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

    addTotalRow(summaryTable, "Total Invoiced:", totalInvoiced, labelFont, valueFont);
    addTotalRow(summaryTable, "Total Paid:", totalPaid, labelFont, valueFont);
    addTotalRow(summaryTable, "Outstanding:", outstanding, labelFont, valueFont);

    document.add(summaryTable);
    document.add(new Paragraph(" "));
}

private void renderTransactionTable(Document document, List<TransactionRow> transactions) 
        throws DocumentException {
    
    Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR);
    Paragraph section = new Paragraph("Transactions", sectionFont);
    section.setSpacingAfter(10);
    document.add(section);

    PdfPTable table = new PdfPTable(5);
    table.setWidthPercentage(100);
    table.setWidths(new float[]{1.5f, 2, 1.5f, 1, 1});

    Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    
    addTableHeader(table, "Date", headerFont);
    addTableHeader(table, "Description", headerFont);
    addTableHeader(table, "Invoice #", headerFont);
    addTableHeader(table, "Amount", headerFont);
    addTableHeader(table, "Status", headerFont);

    Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    boolean altRow = false;
    
    for (TransactionRow row : transactions) {
        addTransactionRow(table, row.date(), row.description(), row.invoiceNumber(), 
                         row.amount(), row.status(), bodyFont, altRow);
        altRow = !altRow;
    }

    document.add(table);
}

private void addTransactionRow(PdfPTable table, String date, String description, String invoiceNumber,
                               String amount, String status, Font font, boolean altRow) {
    
    Color bgColor = altRow ? ROW_ALT : Color.WHITE;

    PdfPCell dateCell = new PdfPCell(new Phrase(date, font));
    dateCell.setBackgroundColor(bgColor);
    dateCell.setPadding(8);
    table.addCell(dateCell);

    PdfPCell descCell = new PdfPCell(new Phrase(description, font));
    descCell.setBackgroundColor(bgColor);
    descCell.setPadding(8);
    table.addCell(descCell);

    PdfPCell invoiceCell = new PdfPCell(new Phrase(invoiceNumber, font));
    invoiceCell.setBackgroundColor(bgColor);
    invoiceCell.setPadding(8);
    table.addCell(invoiceCell);

    PdfPCell amountCell = new PdfPCell(new Phrase(amount, font));
    amountCell.setBackgroundColor(bgColor);
    amountCell.setPadding(8);
    amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    table.addCell(amountCell);

    PdfPCell statusCell = new PdfPCell(new Phrase(status, font));
    statusCell.setBackgroundColor(bgColor);
    statusCell.setPadding(8);
    statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(statusCell);
}

private void renderFooter(Document document) throws DocumentException {
    document.add(new Paragraph(" "));
    
    Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, ACCENT_COLOR);
    Paragraph footer = new Paragraph("Generated: " + java.time.LocalDateTime.now().toString(), footerFont);
    footer.setAlignment(Element.ALIGN_RIGHT);
    document.add(footer);
}

public record TransactionRow(String date, String description, String invoiceNumber, String amount, String status) {}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/PdfRenderer.java
git commit -m "feat(reporting): add customer statement PDF rendering"
```

---

### Task 17: Add Customer Transaction Queries to ReportRepository

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ReportRepository.java`

- [ ] **Step 1: Add CustomerInvoice and CustomerPayment records**

```java
public record CustomerInvoice(
    java.util.UUID invoiceId,
    String invoiceNumber,
    java.math.BigDecimal totalAmount,
    String status,
    java.time.LocalDate issueDate
) {}

public record CustomerPayment(
    java.util.UUID paymentId,
    java.math.BigDecimal amount,
    String status,
    java.time.LocalDate processedAt
) {}
```

- [ ] **Step 2: Add findCustomerInvoices method**

```java
public List<CustomerInvoice> findCustomerInvoices(java.util.UUID customerId) {
    return jdbc.query(
        "SELECT id, invoice_number, total_amount, status, issue_date " +
        "FROM invoices WHERE customer_id = ? ORDER BY issue_date DESC",
        (rs, n) -> new CustomerInvoice(
            rs.getObject("id", java.util.UUID.class),
            rs.getString("invoice_number"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"),
            rs.getDate("issue_date").toLocalDate()
        ),
        customerId
    );
}
```

- [ ] **Step 3: Add findCustomerPayments method**

```java
public List<CustomerPayment> findCustomerPayments(java.util.UUID customerId) {
    return jdbc.query(
        "SELECT id, amount, status, processed_at " +
        "FROM payments WHERE customer_id = ? ORDER BY processed_at DESC",
        (rs, n) -> new CustomerPayment(
            rs.getObject("id", java.util.UUID.class),
            rs.getBigDecimal("amount"),
            rs.getString("status"),
            rs.getDate("processed_at") != null ? rs.getDate("processed_at").toLocalDate() : null
        ),
        customerId
    );
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ReportRepository.java
git commit -m "feat(reporting): add customer transaction queries for statement generation"
```

---

### Task 18: Add Customer Statement PDF Endpoint

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/PdfService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java`

- [ ] **Step 1: Add customer statement generation to PdfService**

```java
public void generateCustomerStatementPdf(OutputStream output, UUID customerId) {
    Customer customer = customerService.findById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(customerId));

    CustomerSummary summary = reportGenerator.forCustomer(customerId);
    
    List<PdfRenderer.TransactionRow> transactions = fetchCustomerTransactions(customerId);

    try {
        pdfRenderer.renderCustomerStatementPdf(
            output,
            customer.getName(),
            "All Time",
            formatAmount(summary.totalInvoiced()),
            formatAmount(summary.totalPaid()),
            formatAmount(summary.totalOutstanding()),
            transactions
        );
    } catch (DocumentException e) {
        throw new RuntimeException("Failed to generate PDF", e);
    }
}

private List<PdfRenderer.TransactionRow> fetchCustomerTransactions(UUID customerId) {
    List<PdfRenderer.TransactionRow> transactions = new ArrayList<>();
    
    // Fetch invoices
    List<ReportRepository.CustomerInvoice> invoices = reportRepository.findCustomerInvoices(customerId);
    for (ReportRepository.CustomerInvoice inv : invoices) {
        transactions.add(new PdfRenderer.TransactionRow(
            inv.issueDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            "Invoice",
            inv.invoiceNumber(),
            formatAmount(inv.totalAmount()),
            inv.status()
        ));
    }
    
    // Fetch payments
    List<ReportRepository.CustomerPayment> payments = reportRepository.findCustomerPayments(customerId);
    for (ReportRepository.CustomerPayment pay : payments) {
        transactions.add(new PdfRenderer.TransactionRow(
            pay.processedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            "Payment",
            "",
            formatAmount(pay.amount()),
            pay.status()
        ));
    }
    
    // Sort by date descending
    transactions.sort((a, b) -> b.date().compareTo(a.date()));
    
    return transactions;
}
```

Add CustomerService dependency:
```java
private final com.ledgerly.customer.CustomerService customerService;

public PdfService(InvoiceDataProvider invoiceDataProvider, PdfRenderer pdfRenderer,
                  com.ledgerly.customer.CustomerService customerService) {
    this.invoiceDataProvider = invoiceDataProvider;
    this.pdfRenderer = pdfRenderer;
    this.customerService = customerService;
}
```

Add ReportGenerator dependency:
```java
private final ReportGenerator reportGenerator;

public PdfService(InvoiceDataProvider invoiceDataProvider, PdfRenderer pdfRenderer,
                  com.ledgerly.customer.CustomerService customerService,
                  ReportGenerator reportGenerator) {
    this.invoiceDataProvider = invoiceDataProvider;
    this.pdfRenderer = pdfRenderer;
    this.customerService = customerService;
    this.reportGenerator = reportGenerator;
}
```

- [ ] **Step 2: Add method to ReportService**

```java
public void generateCustomerStatementPdf(java.io.OutputStream output, java.util.UUID customerId) {
    pdfService.generateCustomerStatementPdf(output, customerId);
}
```

- [ ] **Step 3: Add endpoint to ReportController**

```java
@GetMapping("/customers/{customerId}/statement.pdf")
public void customerStatement(@PathVariable java.util.UUID customerId, HttpServletResponse response) throws IOException {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment; filename=\"statement-" + customerId + ".pdf\"");
    
    reportService.generateCustomerStatementPdf(response.getOutputStream(), customerId);
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/PdfService.java src/main/java/com/ledgerly/reporting/ReportService.java src/main/java/com/ledgerly/reporting/ReportController.java
git commit -m "feat(reporting): add /reports/customers/{id}/statement.pdf endpoint"
```

---

## Phase 4: Excel Generation

### Task 19: Add Apache POI Dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Apache POI dependency to pom.xml**

Add after the OpenPDF dependency:
```xml
<!-- Excel Generation -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

- [ ] **Step 2: Verify dependency resolves**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B dependency:resolve`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: add Apache POI 5.2.5 dependency for Excel generation"
```

---

### Task 20: Create ExcelRenderer

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/ExcelRenderer.java`
- Create: `src/test/java/com/ledgerly/reporting/internal/ExcelRendererTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.ledgerly.reporting.internal;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelRendererTest {

    private final ExcelRenderer renderer = new ExcelRenderer();

    @Test
    void shouldRenderValidExcel() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        Map<String, Long> invoicesByStatus = Map.of("ISSUED", 5L, "PAID", 10L);
        Map<String, Long> paymentsByStatus = Map.of("COMPLETED", 10L);
        
        renderer.renderSummaryExcel(output, 15L, invoicesByStatus, paymentsByStatus, 
            new BigDecimal("5000.00"), new BigDecimal("2000.00"));
        
        byte[] excelBytes = output.toByteArray();
        assertThat(excelBytes).isNotEmpty();
        
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes));
        assertThat(workbook.getNumberOfSheets()).isGreaterThan(0);
        assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("Summary");
        workbook.close();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=ExcelRendererTest`
Expected: FAIL with "ExcelRenderer cannot be resolved"

- [ ] **Step 3: Write minimal implementation**

```java
package com.ledgerly.reporting.internal;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class ExcelRenderer {

    public void renderSummaryExcel(OutputStream output, long totalCustomers,
                                    Map<String, Long> invoicesByStatus,
                                    Map<String, Long> paymentsByStatus,
                                    BigDecimal totalPaid, BigDecimal outstanding) throws Exception {
        
        Workbook workbook = new XSSFWorkbook();
        
        Sheet summarySheet = workbook.createSheet("Summary");
        renderSummarySheet(summarySheet, totalCustomers, invoicesByStatus, paymentsByStatus, totalPaid, outstanding);
        
        workbook.write(output);
        workbook.close();
    }

    private void renderSummarySheet(Sheet sheet, long totalCustomers,
                                     Map<String, Long> invoicesByStatus,
                                     Map<String, Long> paymentsByStatus,
                                     BigDecimal totalPaid, BigDecimal outstanding) {
        
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle dataStyle = createDataStyle(sheet.getWorkbook());
        CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

        int rowNum = 0;

        Row metaRow1 = sheet.createRow(rowNum++);
        metaRow1.createCell(0).setCellValue("Generated:");
        metaRow1.createCell(1).setCellValue(java.time.LocalDateTime.now().toString());

        rowNum++;

        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "Metric", headerStyle);
        createCell(headerRow, 1, "Value", headerStyle);

        createDataRow(sheet, rowNum++, "Total Customers", String.valueOf(totalCustomers), dataStyle);
        
        for (Map.Entry<String, Long> entry : invoicesByStatus.entrySet()) {
            createDataRow(sheet, rowNum++, "Invoices - " + entry.getKey(), String.valueOf(entry.getValue()), dataStyle);
        }
        
        for (Map.Entry<String, Long> entry : paymentsByStatus.entrySet()) {
            createDataRow(sheet, rowNum++, "Payments - " + entry.getKey(), String.valueOf(entry.getValue()), dataStyle);
        }
        
        createDataRow(sheet, rowNum++, "Total Paid", totalPaid.toPlainString(), currencyStyle);
        createDataRow(sheet, rowNum++, "Outstanding", outstanding.toPlainString(), currencyStyle);

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        return workbook.createCellStyle();
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createDataRow(Sheet sheet, int rowNum, String metric, String value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, metric, style);
        createCell(row, 1, value, style);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=ExcelRendererTest`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ExcelRenderer.java src/test/java/com/ledgerly/reporting/internal/ExcelRendererTest.java
git commit -m "feat(reporting): add ExcelRenderer for summary report Excel generation"
```

---

### Task 21: Create ExcelService

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/internal/ExcelService.java`

- [ ] **Step 1: Write ExcelService implementation**

```java
package com.ledgerly.reporting.internal;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class ExcelService {

    private final ExcelRenderer excelRenderer;
    private final ReportGenerator reportGenerator;

    public ExcelService(ExcelRenderer excelRenderer, ReportGenerator reportGenerator) {
        this.excelRenderer = excelRenderer;
        this.reportGenerator = reportGenerator;
    }

    public void generateSummaryExcel(OutputStream output, java.time.LocalDate from, java.time.LocalDate to) {
        OverallSummary summary = reportGenerator.overall(from, to);
        
        try {
            excelRenderer.renderSummaryExcel(
                output,
                summary.totalCustomers(),
                summary.invoicesByStatus(),
                summary.paymentsByStatus(),
                summary.totalAmountPaid(),
                summary.totalOutstanding()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }
}
```

Add import:
```java
import com.ledgerly.reporting.OverallSummary;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ExcelService.java
git commit -m "feat(reporting): add ExcelService for orchestrating Excel generation"
```

---

### Task 22: Add Excel Endpoints

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java`

- [ ] **Step 1: Add method to ReportService**

```java
public void generateSummaryExcel(java.io.OutputStream output, java.time.LocalDate from, java.time.LocalDate to) {
    dateRangeValidator.validate(from, to);
    excelService.generateSummaryExcel(output, from, to);
}
```

Add field and constructor parameter:
```java
private final com.ledgerly.reporting.internal.ExcelService excelService;

public ReportService(com.ledgerly.reporting.internal.ReportGenerator reportGenerator,
                     com.ledgerly.reporting.internal.DateRangeValidator dateRangeValidator,
                     com.ledgerly.reporting.internal.PdfService pdfService,
                     com.ledgerly.reporting.internal.ExcelService excelService) {
    this.reportGenerator = reportGenerator;
    this.dateRangeValidator = dateRangeValidator;
    this.pdfService = pdfService;
    this.excelService = excelService;
}
```

- [ ] **Step 2: Add endpoint to ReportController**

```java
@GetMapping("/summary/excel")
public void summaryExcel(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
                         HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"summary.xlsx\"");
    
    reportService.generateSummaryExcel(response.getOutputStream(), from, to);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportService.java src/main/java/com/ledgerly/reporting/ReportController.java
git commit -m "feat(reporting): add /reports/summary/excel endpoint"
```

---

### Task 23: Extend ExcelRenderer for Aging Reports

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ExcelRenderer.java`

- [ ] **Step 1: Add aging report rendering method**

```java
public void renderAgingExcel(OutputStream output, AgingReport agingReport) throws Exception {
    Workbook workbook = new XSSFWorkbook();
    
    Sheet summarySheet = workbook.createSheet("Aging Summary");
    renderAgingSummarySheet(summarySheet, agingReport);
    
    workbook.write(output);
    workbook.close();
}

private void renderAgingSummarySheet(Sheet sheet, AgingReport agingReport) {
    CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
    CellStyle dataStyle = createDataStyle(sheet.getWorkbook());
    CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

    int rowNum = 0;

    Row headerRow = sheet.createRow(rowNum++);
    createCell(headerRow, 0, "Bucket", headerStyle);
    createCell(headerRow, 1, "Invoice Count", headerStyle);
    createCell(headerRow, 2, "Total Amount", headerStyle);

    for (AgingBucket bucket : agingReport.buckets()) {
        Row row = sheet.createRow(rowNum++);
        createCell(row, 0, bucket.name(), dataStyle);
        createCell(row, 1, String.valueOf(bucket.invoiceCount()), dataStyle);
        createCell(row, 2, bucket.totalAmount().toPlainString(), currencyStyle);
    }

    rowNum++;
    Row totalRow = sheet.createRow(rowNum);
    createCell(totalRow, 0, "Total Outstanding", headerStyle);
    createCell(totalRow, 1, "", dataStyle);
    createCell(totalRow, 2, agingReport.totalOutstanding().toPlainString(), currencyStyle);

    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
    sheet.autoSizeColumn(2);
}
```

Add import:
```java
import com.ledgerly.reporting.AgingBucket;
import com.ledgerly.reporting.AgingReport;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ExcelRenderer.java
git commit -m "feat(reporting): add aging report Excel rendering"
```

---

### Task 24: Add Aging Excel Endpoint

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ExcelService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java`

- [ ] **Step 1: Add method to ExcelService**

```java
public void generateAgingExcel(OutputStream output) {
    AgingReport agingReport = reportGenerator.agingReport();
    
    try {
        excelRenderer.renderAgingExcel(output, agingReport);
    } catch (Exception e) {
        throw new RuntimeException("Failed to generate Excel", e);
    }
}
```

Add import:
```java
import com.ledgerly.reporting.AgingReport;
```

- [ ] **Step 2: Add method to ReportService**

```java
public void generateAgingExcel(java.io.OutputStream output) {
    excelService.generateAgingExcel(output);
}
```

- [ ] **Step 3: Add endpoint to ReportController**

```java
@GetMapping("/aging/excel")
public void agingExcel(HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"aging.xlsx\"");
    
    reportService.generateAgingExcel(response.getOutputStream());
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ExcelService.java src/main/java/com/ledgerly/reporting/ReportService.java src/main/java/com/ledgerly/reporting/ReportController.java
git commit -m "feat(reporting): add /reports/aging/excel endpoint"
```

---

### Task 25: Add Customer Excel Endpoints

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/internal/ExcelRenderer.java`
- Modify: `src/main/java/com/ledgerly/reporting/internal/ExcelService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java`
- Modify: `src/main/java/com/ledgerly/reporting/ReportController.java`

- [ ] **Step 1: Add customer summary rendering to ExcelRenderer**

```java
public void renderCustomerSummaryExcel(OutputStream output, CustomerSummary summary) throws Exception {
    Workbook workbook = new XSSFWorkbook();
    
    Sheet sheet = workbook.createSheet("Customer Summary");
    renderCustomerSummarySheet(sheet, summary);
    
    workbook.write(output);
    workbook.close();
}

private void renderCustomerSummarySheet(Sheet sheet, CustomerSummary summary) {
    CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
    CellStyle dataStyle = createDataStyle(sheet.getWorkbook());
    CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

    int rowNum = 0;

    Row customerRow = sheet.createRow(rowNum++);
    customerRow.createCell(0).setCellValue("Customer:");
    customerRow.createCell(1).setCellValue(summary.customerName());

    rowNum++;

    Row headerRow = sheet.createRow(rowNum++);
    createCell(headerRow, 0, "Metric", headerStyle);
    createCell(headerRow, 1, "Value", headerStyle);

    createDataRow(sheet, rowNum++, "Invoice Count", String.valueOf(summary.invoiceCount()), dataStyle);
    createDataRow(sheet, rowNum++, "Paid Invoice Count", String.valueOf(summary.paidInvoiceCount()), dataStyle);
    createDataRow(sheet, rowNum++, "Total Invoiced", summary.totalInvoiced().toPlainString(), currencyStyle);
    createDataRow(sheet, rowNum++, "Total Paid", summary.totalPaid().toPlainString(), currencyStyle);
    createDataRow(sheet, rowNum++, "Outstanding", summary.totalOutstanding().toPlainString(), currencyStyle);

    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
}
```

Add import:
```java
import com.ledgerly.reporting.CustomerSummary;
```

- [ ] **Step 2: Add method to ExcelService**

```java
public void generateCustomerSummaryExcel(OutputStream output, UUID customerId) {
    CustomerSummary summary = reportGenerator.forCustomer(customerId);
    
    try {
        excelRenderer.renderCustomerSummaryExcel(output, summary);
    } catch (Exception e) {
        throw new RuntimeException("Failed to generate Excel", e);
    }
}
```

- [ ] **Step 3: Add method to ReportService**

```java
public void generateCustomerSummaryExcel(java.io.OutputStream output, java.util.UUID customerId) {
    excelService.generateCustomerSummaryExcel(output, customerId);
}
```

- [ ] **Step 4: Add endpoint to ReportController**

```java
@GetMapping("/customers/{customerId}/excel")
public void customerExcel(@PathVariable java.util.UUID customerId, HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=\"customer-" + customerId + ".xlsx\"");
    
    reportService.generateCustomerSummaryExcel(response.getOutputStream(), customerId);
}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/internal/ExcelRenderer.java src/main/java/com/ledgerly/reporting/internal/ExcelService.java src/main/java/com/ledgerly/reporting/ReportService.java src/main/java/com/ledgerly/reporting/ReportController.java
git commit -m "feat(reporting): add /reports/customers/{id}/excel endpoint"
```

---

## Phase 5: Error Handling & Polish

### Task 26: Create ReportingExceptionHandler

**Files:**
- Create: `src/main/java/com/ledgerly/reporting/ReportingExceptionHandler.java`

- [ ] **Step 1: Write exception handler**

```java
package com.ledgerly.reporting;

import com.ledgerly.customer.CustomerNotFoundException;
import com.ledgerly.invoice.InvoiceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReportingExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCustomerNotFound(CustomerNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericError(Exception ex) {
        return new ErrorResponse("An unexpected error occurred");
    }

    public record ErrorResponse(String error) {}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportingExceptionHandler.java
git commit -m "feat(reporting): add ReportingExceptionHandler for consistent error responses"
```

---

### Task 27: Update package-info.java

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/package-info.java`

- [ ] **Step 1: Update package-info.java**

```java
/**
 * Reporting module: read-only aggregations and document generation (PDF/Excel)
 * across customer, invoice, and payment data. Implemented with raw SQL through
 * {@link com.ledgerly.reporting.internal.ReportRepository} and document
 * rendering libraries (OpenPDF, Apache POI).
 */
@org.springframework.modulith.ApplicationModule(
    id = "reporting",
    displayName = "Reporting & Document Generation",
    allowedDependencies = {"invoice", "customer"}
)
package com.ledgerly.reporting;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/package-info.java
git commit -m "feat(reporting): update package-info with document generation and dependencies"
```

---

### Task 28: Final Integration Test Suite

**Files:**
- Modify: `src/test/java/com/ledgerly/reporting/ReportControllerIntegrationTest.java`

- [ ] **Step 1: Add integration tests for new endpoints**

```java
@Test
void shouldReturnFilteredSummary() {
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 3, 31);
    
    OverallSummary summary = reportService.overallSummary(from, to);
    
    assertThat(summary).isNotNull();
}

@Test
void shouldReturnAgingReport() {
    AgingReport agingReport = reportService.agingReport();
    
    assertThat(agingReport).isNotNull();
    assertThat(agingReport.buckets()).hasSize(5);
}

@Test
void shouldReturnCustomerAgingReport() {
    UUID customerId = UUID.randomUUID();
    
    AgingReport agingReport = reportService.agingReportForCustomer(customerId);
    
    assertThat(agingReport).isNotNull();
    assertThat(agingReport.buckets()).hasSize(5);
}

@Test
void shouldGenerateInvoicePdf() throws Exception {
    UUID invoiceId = createTestInvoice();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    
    reportService.generateInvoicePdf(output, invoiceId);
    
    assertThat(output.toByteArray()).isNotEmpty();
}

@Test
void shouldGenerateSummaryExcel() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    
    reportService.generateSummaryExcel(output, null, null);
    
    assertThat(output.toByteArray()).isNotEmpty();
}

@Test
void shouldGenerateAgingExcel() throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    
    reportService.generateAgingExcel(output);
    
    assertThat(output.toByteArray()).isNotEmpty();
}

private UUID createTestInvoice() {
    // Create a test invoice for PDF generation
    UUID customerId = UUID.randomUUID();
    Invoice invoice = invoiceService.createInvoice(customerId, 
        new BigDecimal("100.00"), new BigDecimal("10.00"), 
        LocalDate.of(2026, 2, 15));
    return invoice.getId();
}
```

Add imports:
```java
import com.ledgerly.reporting.AgingReport;
import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
```

- [ ] **Step 2: Run all tests**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ledgerly/reporting/ReportControllerIntegrationTest.java
git commit -m "test(reporting): add integration tests for Phase 6 features"
```

---

## Verification

After completing all tasks:

1. **Run full test suite:**
   ```bash
   docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test
   ```
   Expected: All tests pass

2. **Build application:**
   ```bash
   docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B clean package
   ```
   Expected: BUILD SUCCESS

3. **Run modularity tests:**
   ```bash
   docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B test -Dtest=ModularityTests
   ```
   Expected: Module structure verification passes

4. **Start application:**
   ```bash
   docker compose down --volumes && docker compose up --build -d
   ```
   Expected: App starts without errors

5. **Test endpoints manually:**
   ```bash
   curl -u ledgerly:ledgerly http://localhost:8080/api/reports/summary
   curl -u ledgerly:ledgerly http://localhost:8080/api/reports/aging
   curl -u ledgerly:ledgerly http://localhost:8080/api/reports/invoices/{id}/pdf -o invoice.pdf
   curl -u ledgerly:ledgerly http://localhost:8080/api/reports/summary/excel -o summary.xlsx
   ```
   Expected: Valid JSON/PDF/Excel responses

---

## Summary

This plan implements Phase 6 of the Ledgerly roadmap in 28 bite-sized tasks across 5 phases:

1. **Date-Range Filtering** (5 tasks) - Extend summary reports with date parameters
2. **Aging Reports** (5 tasks) - Add ledger-wide and per-customer aging calculations
3. **PDF Generation** (8 tasks) - Implement invoice and customer statement PDFs with transaction history
4. **Excel Generation** (7 tasks) - Add Excel export for all report types
5. **Error Handling & Polish** (3 tasks) - Add exception handling and finalize module dependencies

Each task follows TDD principles with explicit test code, implementation code, and verification steps. The plan maintains the modulith architecture while adding document generation capabilities.
