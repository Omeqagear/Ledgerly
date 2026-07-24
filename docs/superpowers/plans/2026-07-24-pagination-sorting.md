# Phase 9a: Pagination & Sorting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add offset pagination and sorting to the customer, invoice, payment, and user list endpoints via Spring Data `Pageable` / `Page<T>`.

**Architecture:** Add `Page<T>`-returning service methods backed by Spring Data repository query methods that accept `Pageable`. Controllers inject `Pageable` (auto-resolved from `?page`/`?size`/`?sort` query params) and return `Page<T>`. Existing `List<T>` service methods are kept for internal callers (scheduled jobs, cross-module lookups). No new dependencies — `spring-boot-starter-data-jpa` already brings `Pageable` web support.

**Tech Stack:** Spring Boot 3.2.5, Spring Data JPA (`Pageable`, `Page`, `Sort`), H2 (tests), PostgreSQL (prod)

**Design spec:** `docs/superpowers/specs/2026-07-24-pagination-sorting-design.md`

---

## File Structure

**Modified files:**
- `src/main/resources/application.yml` — pagination defaults + max size
- `src/test/resources/application.yml` — same defaults for tests
- `src/main/java/com/ledgerly/customer/CustomerService.java` — add `findAll(Pageable)`
- `src/main/java/com/ledgerly/customer/CustomerController.java` — inject `Pageable`, return `Page<Customer>`
- `src/main/java/com/ledgerly/invoice/internal/InvoiceRepository.java` — add paginated query methods
- `src/main/java/com/ledgerly/invoice/InvoiceService.java` — add paginated methods
- `src/main/java/com/ledgerly/invoice/InvoiceController.java` — inject `Pageable`, return `Page<Invoice>`
- `src/main/java/com/ledgerly/payment/internal/PaymentRepository.java` — add paginated query methods
- `src/main/java/com/ledgerly/payment/PaymentService.java` — add paginated methods + `findAll(Pageable)`
- `src/main/java/com/ledgerly/payment/PaymentController.java` — inject `Pageable`, return `Page<Payment>`
- `src/main/java/com/ledgerly/user/UserService.java` — add `findAll(Pageable)`
- `src/main/java/com/ledgerly/user/UserController.java` — inject `Pageable`, return `Page<UserResponse>`
- `README.md` — document pagination query params

**New test files:**
- `src/test/java/com/ledgerly/customer/CustomerPaginationIntegrationTest.java`
- `src/test/java/com/ledgerly/invoice/InvoicePaginationIntegrationTest.java`
- `src/test/java/com/ledgerly/payment/PaymentPaginationIntegrationTest.java`
- `src/test/java/com/ledgerly/user/UserPaginationIntegrationTest.java`
- `src/test/java/com/ledgerly/PaginationWebIntegrationTest.java`

---

## Task 1: Configure pagination defaults

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application.yml`

- [ ] **Step 1: Add pagination config to main application.yml**

Add this block under the existing `spring:` key in `src/main/resources/application.yml` (merge into the `spring:` section):

```yaml
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

Place it as a sibling of `datasource:`, `jpa:`, etc. under `spring:`.

- [ ] **Step 2: Add the same block to test application.yml**

Add to `src/test/resources/application.yml` under the `spring:` key:

```yaml
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

- [ ] **Step 3: Verify the build still passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test`
Expected: BUILD SUCCESS (no behavioral change yet, just config)

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yml src/test/resources/application.yml
git commit -m "feat(config): add pagination defaults (page size 20, max 100)"
```

---

## Task 2: Customer module pagination

**Files:**
- Modify: `src/main/java/com/ledgerly/customer/CustomerService.java`
- Modify: `src/main/java/com/ledgerly/customer/CustomerController.java`
- Create: `src/test/java/com/ledgerly/customer/CustomerPaginationIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/ledgerly/customer/CustomerPaginationIntegrationTest.java`:

```java
package com.ledgerly.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.test.ApplicationModuleTest;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
class CustomerPaginationIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Test
    void shouldPaginateFindAll() {
        for (int i = 0; i < 5; i++) {
            customerService.createCustomer("C" + i, "c" + i + "@example.com", null, null);
        }

        Page<Customer> page = customerService.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(3);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void shouldSortByNameAscending() {
        customerService.createCustomer("Zebra", "zebra@example.com", null, null);
        customerService.createCustomer("Alpha", "alpha@example.com", null, null);

        Page<Customer> page = customerService.findAll(
            PageRequest.of(0, 10, Sort.by("name").ascending()));

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getName()).isEqualTo("Alpha");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=CustomerPaginationIntegrationTest`
Expected: FAIL with "cannot find symbol method findAll(PageRequest)" in `CustomerService`

- [ ] **Step 3: Add `findAll(Pageable)` to CustomerService**

In `src/main/java/com/ledgerly/customer/CustomerService.java`, add the import and method. Add to the imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add this method after the existing `findAll()` (around line 60):

```java
    @Transactional(readOnly = true)
    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }
```

`CustomerRepository extends JpaRepository` which already provides `findAll(Pageable)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=CustomerPaginationIntegrationTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Update CustomerController to return `Page<Customer>`**

In `src/main/java/com/ledgerly/customer/CustomerController.java`, replace the imports and `list` method. Replace `import java.util.List;` with:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replace the `list` method:

```java
    @GetMapping
    public Page<Customer> list(Pageable pageable) {
        return customerService.findAll(pageable);
    }
```

- [ ] **Step 6: Run the full customer test suite**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=CustomerModuleIntegrationTests,CustomerPaginationIntegrationTest,SecurityIntegrationTest`
Expected: PASS (SecurityIntegrationTest hits `GET /customers` and only asserts status 200 — `Page` serializes fine)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ledgerly/customer/CustomerService.java src/main/java/com/ledgerly/customer/CustomerController.java src/test/java/com/ledgerly/customer/CustomerPaginationIntegrationTest.java
git commit -m "feat(customer): add pagination and sorting to GET /customers"
```

---

## Task 3: Invoice module pagination

**Files:**
- Modify: `src/main/java/com/ledgerly/invoice/internal/InvoiceRepository.java`
- Modify: `src/main/java/com/ledgerly/invoice/InvoiceService.java`
- Modify: `src/main/java/com/ledgerly/invoice/InvoiceController.java`
- Create: `src/test/java/com/ledgerly/invoice/InvoicePaginationIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/ledgerly/invoice/InvoicePaginationIntegrationTest.java`:

```java
package com.ledgerly.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
class InvoicePaginationIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void shouldPaginateFindAll() {
        UUID customerId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            invoiceService.createInvoice(
                customerId, new BigDecimal("100.00"), BigDecimal.ONE, LocalDate.now().plusDays(30));
        }

        Page<Invoice> page = invoiceService.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(5);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void shouldPaginateByCustomerId() {
        UUID customerId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            invoiceService.createInvoice(
                customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        }

        Page<Invoice> page = invoiceService.findByCustomerId(customerId, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void shouldSortByInvoiceNumberDescending() {
        UUID customerId = UUID.randomUUID();
        invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));

        Page<Invoice> page = invoiceService.findAll(
            PageRequest.of(0, 10, Sort.by("invoiceNumber").descending()));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getInvoiceNumber())
            .isGreaterThan(page.getContent().get(1).getInvoiceNumber());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=InvoicePaginationIntegrationTest`
Expected: FAIL with "cannot find symbol method findAll(PageRequest)" in `InvoiceService`

- [ ] **Step 3: Add paginated methods to InvoiceRepository**

In `src/main/java/com/ledgerly/invoice/internal/InvoiceRepository.java`, add the `Page` import and two new methods. Add to imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add after the existing `findOverdueInvoices()` method:

```java
    Page<Invoice> findByCustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.status = com.ledgerly.invoice.InvoiceStatus.ISSUED AND i.dueDate < CURRENT_DATE")
    Page<Invoice> findOverdueInvoices(Pageable pageable);
```

Note: the existing `List<Invoice> findOverdueInvoices()` (no `Pageable`) is kept for `OverdueInvoiceMarker`. Spring Data resolves the overload by parameter signature.

- [ ] **Step 4: Add paginated methods to InvoiceService**

In `src/main/java/com/ledgerly/invoice/InvoiceService.java`, add imports and methods. Add to imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add after the existing `findAll()` method (around line 90):

```java
    @Transactional(readOnly = true)
    public Page<Invoice> findAll(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> findByCustomerId(UUID customerId, Pageable pageable) {
        return invoiceRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> findOverdueInvoices(Pageable pageable) {
        return invoiceRepository.findOverdueInvoices(pageable);
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=InvoicePaginationIntegrationTest`
Expected: PASS (3 tests)

- [ ] **Step 6: Update InvoiceController to return `Page<Invoice>`**

In `src/main/java/com/ledgerly/invoice/InvoiceController.java`, replace `import java.util.List;` with:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replace the `list` method:

```java
    @GetMapping
    public Page<Invoice> list(@RequestParam(value = "customerId", required = false) UUID customerId,
                              @RequestParam(value = "overdue", required = false, defaultValue = "false") boolean overdue,
                              Pageable pageable) {
        if (overdue) {
            return invoiceService.findOverdueInvoices(pageable);
        }
        if (customerId != null) {
            return invoiceService.findByCustomerId(customerId, pageable);
        }
        return invoiceService.findAll(pageable);
    }
```

- [ ] **Step 7: Run the invoice + modularity tests**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=InvoiceModuleIntegrationTests,InvoicePaginationIntegrationTest,ModularityTests`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/ledgerly/invoice/internal/InvoiceRepository.java src/main/java/com/ledgerly/invoice/InvoiceService.java src/main/java/com/ledgerly/invoice/InvoiceController.java src/test/java/com/ledgerly/invoice/InvoicePaginationIntegrationTest.java
git commit -m "feat(invoice): add pagination and sorting to GET /invoices"
```

---

## Task 4: Payment module pagination

**Files:**
- Modify: `src/main/java/com/ledgerly/payment/internal/PaymentRepository.java`
- Modify: `src/main/java/com/ledgerly/payment/PaymentService.java`
- Modify: `src/main/java/com/ledgerly/payment/PaymentController.java`
- Create: `src/test/java/com/ledgerly/payment/PaymentPaginationIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/ledgerly/payment/PaymentPaginationIntegrationTest.java`:

```java
package com.ledgerly.payment;

import com.ledgerly.invoice.Invoice;
import com.ledgerly.invoice.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentPaginationIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void shouldPaginateFindAll() {
        UUID customerId = UUID.randomUUID();
        payInvoice(customerId);
        payInvoice(customerId);
        payInvoice(customerId);

        Page<Payment> page = paymentService.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void shouldPaginateByCustomerId() {
        UUID customerId = UUID.randomUUID();
        payInvoice(customerId);
        payInvoice(customerId);

        Page<Payment> page = paymentService.findByCustomerId(customerId, PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    private void payInvoice(UUID customerId) {
        Invoice invoice = invoiceService.createInvoice(
            customerId, BigDecimal.TEN, BigDecimal.ONE, LocalDate.now().plusDays(30));
        invoiceService.issueInvoice(invoice.getId());
        paymentService.processPayment(
            invoice.getId(), customerId, BigDecimal.TEN, "CARD", "TXN-" + UUID.randomUUID());
    }
}
```

Note: this test uses `@SpringBootTest` (like the existing `PaymentModuleIntegrationTests`) because the payment module depends on invoice and needs the full context for the payment flow (gateway stub + event publication).

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=PaymentPaginationIntegrationTest`
Expected: FAIL with "cannot find symbol method findAll(PageRequest)" in `PaymentService`

- [ ] **Step 3: Add paginated methods to PaymentRepository**

In `src/main/java/com/ledgerly/payment/internal/PaymentRepository.java`, add the `Page` import and paginated query methods. Add to imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replace the two existing methods with paginated variants (the `List` versions were only called by the controller which is being updated):

```java
    Page<Payment> findByInvoiceId(UUID invoiceId, Pageable pageable);

    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);
```

- [ ] **Step 4: Add paginated methods to PaymentService**

In `src/main/java/com/ledgerly/payment/PaymentService.java`, add imports and methods. Add to imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replace the existing `findByInvoiceId` and `findByCustomerId` methods (lines ~89-99) with paginated versions, and add `findAll(Pageable)`:

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByInvoiceId(UUID invoiceId, Pageable pageable) {
        return paymentRepository.findByInvoiceId(invoiceId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findByCustomerId(UUID customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable);
    }
```

The `PaymentAPI` interface currently declares `List<Payment> findByInvoiceId(UUID)` and `List<Payment> findByCustomerId(UUID)`, but no other module calls them (the reporting module uses raw SQL via `ReportRepository`, not `PaymentAPI`). Remove those two declarations from `PaymentAPI` so only `findById` remains on the interface. The paginated filtered methods stay on `PaymentService` only (controller-facing).

Edit `src/main/java/com/ledgerly/payment/PaymentAPI.java` to:

```java
package com.ledgerly.payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-side contract for other modules that need to inspect payment records
 * (e.g. reporting).
 */
public interface PaymentAPI {

    Optional<Payment> findById(UUID id);
}
```

Remove the `import java.util.List;` (no longer used).

- [ ] **Step 5: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=PaymentPaginationIntegrationTest`
Expected: PASS (2 tests)

- [ ] **Step 6: Update PaymentController to return `Page<Payment>`**

In `src/main/java/com/ledgerly/payment/PaymentController.java`, replace `import java.util.List;` with:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replace the `list` method. The no-filter branch now returns `findAll(pageable)` instead of `List.of()`:

```java
    @GetMapping
    public Page<Payment> list(@RequestParam(value = "invoiceId", required = false) UUID invoiceId,
                              @RequestParam(value = "customerId", required = false) UUID customerId,
                              Pageable pageable) {
        if (invoiceId != null) {
            return paymentService.findByInvoiceId(invoiceId, pageable);
        }
        if (customerId != null) {
            return paymentService.findByCustomerId(customerId, pageable);
        }
        return paymentService.findAll(pageable);
    }
```

- [ ] **Step 7: Run the payment + modularity tests**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=PaymentModuleIntegrationTests,PaymentPaginationIntegrationTest,ModularityTests`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/ledgerly/payment/internal/PaymentRepository.java src/main/java/com/ledgerly/payment/PaymentService.java src/main/java/com/ledgerly/payment/PaymentController.java src/main/java/com/ledgerly/payment/PaymentAPI.java src/test/java/com/ledgerly/payment/PaymentPaginationIntegrationTest.java
git commit -m "feat(payment): add pagination and sorting to GET /payments"
```

---

## Task 5: User module pagination

**Files:**
- Modify: `src/main/java/com/ledgerly/user/UserService.java`
- Modify: `src/main/java/com/ledgerly/user/UserController.java`
- Create: `src/test/java/com/ledgerly/user/UserPaginationIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/ledgerly/user/UserPaginationIntegrationTest.java`:

```java
package com.ledgerly.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.modulith.test.ApplicationModuleTest;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
class UserPaginationIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void shouldPaginateFindAll() {
        // The seeded admin user already exists
        userService.createUser("pager-1", "password123", "USER");
        userService.createUser("pager-2", "password123", "USER");

        Page<User> page = userService.findAll(PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getSize()).isEqualTo(1);
    }
}
```

Note: `SeedUserRunner` creates an `admin` user on startup, so at least one user exists. The test adds two more and asserts the first page of size 1 contains one element and the total is at least 3.

- [ ] **Step 2: Run test to verify it fails**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=UserPaginationIntegrationTest`
Expected: FAIL with "cannot find symbol method findAll(PageRequest)" in `UserService`

- [ ] **Step 3: Add `findAll(Pageable)` to UserService**

In `src/main/java/com/ledgerly/user/UserService.java`, add imports and method. Add to imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add after the existing `findAll()` (around line 47):

```java
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=UserPaginationIntegrationTest`
Expected: PASS (1 test)

- [ ] **Step 5: Update UserController to return `Page<UserResponse>`**

In `src/main/java/com/ledgerly/user/UserController.java`, replace `import java.util.List;` with:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replace the `list` method:

```java
    @GetMapping
    public Page<UserResponse> list(Pageable pageable) {
        return userService.findAll(pageable).map(UserResponse::from);
    }
```

`Page.map(...)` transforms the page content to `UserResponse` while preserving pagination metadata.

- [ ] **Step 6: Run the user + security tests**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=UserServiceTest,UserPaginationIntegrationTest,SecurityIntegrationTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ledgerly/user/UserService.java src/main/java/com/ledgerly/user/UserController.java src/test/java/com/ledgerly/user/UserPaginationIntegrationTest.java
git commit -m "feat(user): add pagination and sorting to GET /users"
```

---

## Task 6: HTTP-level pagination integration test

**Files:**
- Create: `src/test/java/com/ledgerly/PaginationWebIntegrationTest.java`

This test verifies that Spring resolves `?page`/`?size`/`?sort` query params into `Pageable`, that the response is a `Page` JSON object with `content`/`totalElements`/`totalPages`, and that sorting is applied. It runs with the default `dev` profile (open endpoints).

- [ ] **Step 1: Write the test**

Create `src/test/java/com/ledgerly/PaginationWebIntegrationTest.java`:

```java
package com.ledgerly;

import com.ledgerly.customer.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaginationWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @BeforeEach
    void seedCustomers() {
        customerService.createCustomer("Zulu", "zulu-pag@example.com", null, null);
        customerService.createCustomer("Alpha", "alpha-pag@example.com", null, null);
        customerService.createCustomer("Mike", "mike-pag@example.com", null, null);
    }

    @Test
    void shouldReturnPageJsonWithPagination() throws Exception {
        mockMvc.perform(get("/customers").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").exists())
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.totalPages").isNumber())
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldApplySortQueryParam() throws Exception {
        mockMvc.perform(get("/customers")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Alpha"));
    }

    @Test
    void shouldClampSizeToMax() throws Exception {
        mockMvc.perform(get("/customers").param("size", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100));
    }
}
```

Note: `seedCustomers` runs before each test and may create duplicates across tests only if emails collide — emails here are unique per test method only if `@BeforeEach` runs once per test. Since `@BeforeEach` runs before every test, the second test's `seedCustomers` would try to create the same emails and throw `DuplicateCustomerEmailException`. To avoid this, use unique emails per test by reading the current test name, OR guard with `findByEmail`. Simplest fix: wrap each create in a try/catch that ignores `DuplicateCustomerEmailException`:

```java
    @BeforeEach
    void seedCustomers() {
        safeCreate("Zulu", "zulu-pag@example.com");
        safeCreate("Alpha", "alpha-pag@example.com");
        safeCreate("Mike", "mike-pag@example.com");
    }

    private void safeCreate(String name, String email) {
        try {
            customerService.createCustomer(name, email, null, null);
        } catch (DuplicateCustomerEmailException ignored) {
            // already seeded by a previous test
        }
    }
```

Add the import: `import com.ledgerly.customer.DuplicateCustomerEmailException;`

- [ ] **Step 2: Run the test**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=PaginationWebIntegrationTest`
Expected: PASS (3 tests)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ledgerly/PaginationWebIntegrationTest.java
git commit -m "test: add HTTP-level pagination query-param and sort integration test"
```

---

## Task 7: Update README and run full suite

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document pagination in README**

In `README.md`, add a subsection under the "API overview" section (after the User module table), before "### Customer module" or at the end of the API overview. Add:

```markdown
### Pagination & sorting

All list endpoints (`GET /customers`, `GET /invoices`, `GET /payments`,
`GET /users`) accept these query parameters:

| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Zero-indexed page number |
| `size` | `20` | Page size (max `100`) |
| `sort` | natural | `field,dir` — e.g. `sort=name,asc` or `sort=createdAt,desc`; repeatable |

Responses are Spring `Page` objects (`{ content, totalElements, totalPages, ... }`), not bare arrays.

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/customers?page=0&size=10&sort=name,asc"
```
```

- [ ] **Step 2: Run the full test suite**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp clean test`
Expected: BUILD SUCCESS — all existing tests + the 5 new pagination test classes pass

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document pagination and sorting query params for list endpoints"
```

---

## Verification

After completing all tasks:

1. **Full test suite:**
   ```bash
   docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp clean test
   ```
   Expected: BUILD SUCCESS

2. **Smoke-test pagination (dev profile):**
   ```bash
   curl "http://localhost:8080/api/customers?page=0&size=5&sort=name,asc" | jq '.totalElements, .content[0].name'
   ```

3. **Verify size clamping:**
   ```bash
   curl "http://localhost:8080/api/customers?size=999" | jq '.size'  # → 100
   ```

4. **Verify modularity still holds** — `ModularityTests` is part of the test suite and must pass.

---

## Summary

This plan implements Phase 9a in 7 tasks:

1. **Config** (Task 1) — pagination defaults + max size
2. **Customer** (Task 2) — `findAll(Pageable)`, controller, tests
3. **Invoice** (Task 3) — `findAll`/`findByCustomerId`/`findOverdueInvoices` paginated, controller, tests
4. **Payment** (Task 4) — `findAll`/`findByInvoiceId`/`findByCustomerId` paginated, no-filter behavior fix, tests
5. **User** (Task 5) — `findAll(Pageable)`, `Page.map(UserResponse::from)`, tests
6. **HTTP integration** (Task 6) — MockMvc test for query-param binding, sort, size clamp
7. **Docs** (Task 7) — README pagination section + full suite green

Each task follows TDD with explicit test code, implementation, and verification. The existing `List<T>` service methods are preserved so internal callers (scheduled jobs, cross-module lookups) are unaffected.
