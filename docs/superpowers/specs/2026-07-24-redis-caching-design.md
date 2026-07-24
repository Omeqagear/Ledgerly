# Phase 9b — Redis Caching for Customer Lookups and Report Summaries

**Date:** 2026-07-24
**Status:** Design approved

## Overview

Add Spring Cache abstraction backed by Redis to Ledgerly. Cache customer lookups
(used by the notification module via `CustomerLookup`) and report summaries
(used by the reporting module via `ReportService`). PDF/Excel binary stream
generation is not cached.

## Cache Targets

### 1. CustomerLookup (`customers` cache, eviction-based)

| Method | Cache key | Eviction |
|---|---|---|
| `findEmailById(UUID)` | `#customerId` | `@CacheEvict` on create/update/delete |
| `findInfoById(UUID)` | `#customerId` | `@CacheEvict` on create/update/delete |
| `exists(UUID)` | `#customerId` | `@CacheEvict` on create/update/delete |

- Cache region: `customers`
- No TTL — entries remain valid until explicitly evicted
- Eviction triggered by `CustomerService.createCustomer`, `updateCustomer`, `deleteCustomer`

### 2. ReportService (`reports` cache, TTL-based)

| Method | Cache key |
|---|---|
| `overallSummary(null, null)` | `'summary-all-all'` |
| `overallSummary(from, to)` | `'summary-' + (#from ?: 'all') + '-' + (#to ?: 'all')` |
| `customerSummary(UUID)` | `'cust-summary-' + #customerId` |
| `agingReport()` | `'aging'` |
| `agingReportForCustomer(UUID)` | `'aging-cust-' + #customerId` |

- Cache region: `reports`
- TTL: 5 minutes
- No manual eviction — reports are inherently point-in-time snapshots; a 5-min
  staleness window is acceptable

### 3. Not cached

- `generateInvoicePdf`, `generateCustomerStatementPdf` — binary streams
- `generateSummaryExcel`, `generateAgingExcel`, `generateCustomerSummaryExcel` — binary streams

## Architecture Changes

### Dependencies (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- Testcontainers Redis for cache integration tests -->
<dependency>
    <groupId>com.redis</groupId>
    <artifactId>testcontainers-redis</artifactId>
    <version>2.2.2</version>
    <scope>test</scope>
</dependency>
```

### Configuration (`CacheConfig`)

New class `com.ledgerly.config.CacheConfig`:
- `@Configuration` + `@EnableCaching`
- `RedisCacheConfiguration` bean defining default TTL and key serialization
- Customizer beans per cache region (`customers` — no TTL; `reports` — 5 min TTL)

### Docker Compose

```yaml
redis:
  image: redis:7-alpine
  container_name: ledgerly-redis
  ports:
    - "6379:6379"
  networks:
    - ledgerly-network
  restart: unless-stopped
```

### Application config (`application.yml`)

```yaml
spring:
  data:
    redis:
      host: ${LEDGERLY_REDIS_HOST:localhost}
      port: ${LEDGERLY_REDIS_PORT:6379}
```

## Source Changes

| File | Change |
|---|---|
| `pom.xml` | Add `spring-boot-starter-cache`, `spring-boot-starter-data-redis`, `testcontainers-redis` |
| `docker-compose.yml` | Add `redis` service |
| `application.yml` | Add `spring.data.redis` connection config |
| `config/CacheConfig.java` | NEW — `@EnableCaching` + Redis cache configuration |
| `customer/internal/CustomerLookupImpl.java` | Add `@Cacheable` on `findEmailById`, `findInfoById`, `exists` |
| `customer/CustomerService.java` | Add `@CacheEvict` on `createCustomer`, `updateCustomer`, `deleteCustomer` |
| `reporting/ReportService.java` | Add `@Cacheable` on summary methods |
| `LedgerlyApplication.java` | No change (auto-config handles Redis connection) |

## Testing

### Existing tests — no changes

The 101 existing tests run against H2 with Flyway disabled and no Redis config.
No modification is needed. The `@ApplicationModuleTest` suites are unaffected.

### New test: `CachingIntegrationTest`

- `@SpringBootTest` with a profile that enables Redis via Testcontainers
- Creates customers, verifies cache hits via `CacheManager` inspection
- Creates and verifies report caching
- Verifies eviction: update customer → cache miss on subsequent lookup

### Redis in test `application.yml`

Test `application.yml` does NOT add Redis config. The caching integration test
uses `@DynamicPropertySource` to wire Testcontainers Redis at runtime.

## Profiles

| Profile | Redis behavior |
|---|---|
| `dev` (default) | Redis connection attempted; app starts even if Redis is unavailable |
| `prod` | Redis connection required (app fails fast if Redis is down) |
| Test (`@ApplicationModuleTest`) | No Redis — caching disabled |
| Test (`@SpringBootTest` with Redis profile) | Testcontainers Redis |

## Edge Cases

- **Redis unavailable in dev**: App starts, caching degrades to no-op, logs a warning
- **Redis unavailable in prod**: Startup fails — Redis is required
- **Cache key collision**: Keys are namespaced by cache region (`customers::<id>`, `reports::<key>`) preventing collisions
- **Large cache entries**: Report DTOs are modest in size (summary counts + maps); Redis memory usage is bounded

## Descoped

- Distributed session management via Redis
- Cache for individual invoice/payment lookups (they're paginated and less hot)
- Cache monitoring dashboard (deferred to observability phase)
