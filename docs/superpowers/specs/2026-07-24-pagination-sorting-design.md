# Phase 9a: Pagination & Sorting Design Document

## Overview

Add pagination and sorting to all list endpoints in the customer, invoice,
payment, and user modules using Spring Data's `Pageable` / `Page<T>` / `Sort`
abstractions. This is the first sub-plan of Phase 9; Redis caching, load
testing, query optimization, and resource tuning are deferred to later
sub-plans (9b–9e).

## Scope

- Replace `List<T>` list responses with `Page<T>` on four modules
- Support `?page=`, `?size=`, and `?sort=field,dir` query parameters
- Cap page size to prevent abuse; default to 20 rows
- Preserve existing filtered query behavior (invoice `customerId`/`overdue`,
  payment `invoiceId`/`customerId`) with pagination applied
- No new dependencies (Spring Data JPA already on the classpath brings
  `Pageable` web support)
- No changes to the reporting module (aggregations, not list endpoints)

## Breaking API change

List endpoint JSON responses change from an array to a Spring `Page` object:

```
# Before
[ { "id": "...", ... }, { "id": "...", ... } ]

# After
{
  "content": [ { "id": "...", ... } ],
  "pageable": { "pageNumber": 0, "pageSize": 20, ... },
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  ...
}
```

This is acceptable for the current scaffold (no external consumers). Clients
must read `.content` and use `.totalElements` / `.totalPages` for navigation.

## Endpoints affected

| Method | Endpoint | Current return | New return |
|--------|----------|----------------|------------|
| `GET` | `/customers` | `List<Customer>` | `Page<Customer>` |
| `GET` | `/invoices?customerId=&overdue=` | `List<Invoice>` | `Page<Invoice>` |
| `GET` | `/payments?invoiceId=&customerId=` | `List<Payment>` | `Page<Payment>` |
| `GET` | `/users` | `List<UserResponse>` | `Page<UserResponse>` |

## Pagination semantics

- `page`: zero-indexed page number (default `0`)
- `size`: page size (default `20`, max `100` — Spring clamps oversize requests)
- `sort`: `field,dir` where `dir` is `asc` or `desc` (e.g. `sort=name,asc`).
  Repeatable for multi-field sort: `sort=name,asc&sort=createdAt,desc`.
- Omitting all three returns the first 20 rows sorted by natural order.

Spring Boot auto-configures `PageableHandlerMethodArgumentResolver` from
`spring-data-commons` (transitive via `spring-boot-starter-data-jpa`), so
controller methods can accept `Pageable` directly with no extra wiring.

## Sortable fields

Sorting is constrained to JPA entity fields (Spring Data validates property
paths). The notable sortable fields per entity:

| Entity | Sortable fields |
|--------|-----------------|
| `Customer` | `name`, `email`, `createdAt` |
| `Invoice` | `invoiceNumber`, `issueDate`, `dueDate`, `totalAmount`, `createdAt` |
| `Payment` | `amount`, `processedAt`, `createdAt` |
| `User` | `username`, `createdAt` |

Requesting an unsortable/non-existent field returns 400 (Spring Data's
default behavior).

## Architecture

### Approach: add paginated service methods, keep internal callers stable

Existing `List<T>` service methods (`findAll`, `findByCustomerId`, etc.) are
called only by controllers today — internal module callers (reporting's
`InvoiceDataProvider`, payment's `InvoiceAPI.findById`) use `findById` /
single-entity lookups. To keep the blast radius small and preserve the
module API surface, the plan **adds** `Page<T>`-returning service methods
rather than replacing the `List<T>` ones:

- `Page<Customer> findAll(Pageable pageable)`
- `Page<Invoice> findAll(Pageable)` + `Page<Invoice> findByCustomerId(UUID, Pageable)` + `Page<Invoice> findOverdueInvoices(Pageable)`
- `Page<Payment> findAll(Pageable)` + `Page<Payment> findByInvoiceId(UUID, Pageable)` + `Page<Payment> findByCustomerId(UUID, Pageable)`
- `Page<User> findAll(Pageable)`

The controllers switch to the paginated methods. The existing `List<T>`
methods remain for any future internal callers that want unpaginated access.

### Repository changes

`JpaRepository` already provides `findAll(Pageable)`. Filtered queries need
new methods accepting `Pageable`:

- `InvoiceRepository`: `Page<Invoice> findByCustomerId(UUID, Pageable)`;
  `findOverdueInvoices()` becomes `Page<Invoice> findOverdueInvoices(Pageable)`
  (the `@Query` is kept, signature gains a `Pageable` parameter — Spring Data
  returns a `Page` and adds count + limit clauses automatically)
- `PaymentRepository`: `Page<Payment> findByInvoiceId(UUID, Pageable)`;
  `Page<Payment> findByCustomerId(UUID, Pageable)`

### Payment list behavior fix

`PaymentController.list` currently returns `List.of()` when no filter is
supplied. With pagination, the no-filter branch returns `findAll(pageable)` —
a real first page of all payments. This is a deliberate behavior correction,
not just a shape change.

## Configuration

Add to `src/main/resources/application.yml` and `src/test/resources/application.yml`:

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```

`spring.data.web.pageable.max-page-size` caps `size`; Spring clamps requests
exceeding it rather than erroring. Negative `page` values are rejected with
400 by Spring.

## Testing strategy

**Service-level (module integration tests, H2):** seed multiple entities,
call the paginated service method with a `PageRequest`, assert `getContent`
size, `getTotalElements`, and `getTotalPages`.

**Controller-level (MockMvc, one `@SpringBootTest`):** verify query-param
binding — `?page=0&size=2&sort=name,asc` produces a `Page` JSON with the
expected `content`/`totalElements` and sorted order.

The existing module integration tests call services directly and do not
assert on list JSON shape, so they are unaffected by the `List` → `Page`
controller change.

## Out of scope

- Redis caching (Phase 9b)
- Load/performance tests (Phase 9c)
- Query optimization / indexes (Phase 9d)
- Resource tuning (Phase 9e)
- Reporting module endpoints (aggregations, not paginated lists)
- Cursor/keyset pagination (offset pagination via `Pageable` is sufficient
  for the current data volume)
