# Phase 9b — Redis Caching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Spring Cache backed by Redis for customer lookups and report summaries, with testcontainers for integration testing.

**Architecture:** `spring-boot-starter-data-redis` provides the `RedisCacheManager`; a new `CacheConfig` class enables caching and configures per-region TTLs. Customer lookups are evicted on mutation; report summaries use 5-min TTL. Dev profile uses in-memory simple cache; prod uses Redis.

**Tech Stack:** Spring Boot 3.2.5, spring-boot-starter-data-redis, Testcontainers Redis, JUnit 5

---

### Task 1: Add Redis dependencies to pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add `spring-boot-starter-data-redis` dependency**

Insert after the PDF generation dependency block (line 74), before the Excel generation block:

```xml
        <!-- Caching -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
```

The exact insertion point is after `</dependency>` closing the openpdf dependency and before `<!-- Excel generation -->`.

- [ ] **Step 2: Add `testcontainers-redis` test dependency**

Insert after the `postgresql` testcontainers dependency (line 130), before the `<!-- H2 -->` comment:

```xml
        <dependency>
            <groupId>com.redis</groupId>
            <artifactId>testcontainers-redis</artifactId>
            <version>2.2.2</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 3: Verify dependencies resolve**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp dependency:resolve`

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "feat: add redis and testcontainers-redis dependencies"
```

---

### Task 2: Add Redis service to docker-compose.yml

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add redis service definition**

Insert after the `prometheus` service block (after line 51, before `volumes:`):

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

- [ ] **Step 2: Add Redis env vars to app service**

In the `app` service's `environment` block, add after line 14 (after `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`):

```yaml
      - SPRING_CACHE_TYPE=redis
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_DATA_REDIS_PORT=6379
```

- [ ] **Step 3: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add redis service to docker compose"
```

---

### Task 3: Configure Redis caching in application.yml

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add cache and Redis config**

Insert after `spring.` block and before the `datasource` config (after line 13, before line 14):

```yaml
  cache:
    type: ${LEDGERLY_CACHE_TYPE:simple}
  data:
    redis:
      host: ${LEDGERLY_REDIS_HOST:localhost}
      port: ${LEDGERLY_REDIS_PORT:6379}
```

The `spring:` block currently starts at line 1. The new config is inserted as additional properties under `spring:`. Insert right after the `spring.data.web.pageable` block (after line 12's `max-page-size: 100`) and before `spring.datasource:` (line 14).

In context, the file beginning becomes:

```yaml
spring:
  application:
    name: ledgerly

  profiles:
    active: dev

  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100

  cache:
    type: ${LEDGERLY_CACHE_TYPE:simple}

  datasource:
```

And after `spring.datasource:` block, before `spring.jpa:`, insert:

```yaml
  data:
    redis:
      host: ${LEDGERLY_REDIS_HOST:localhost}
      port: ${LEDGERLY_REDIS_PORT:6379}
```

Wait — there's already a `spring.data` block for `web.pageable`. The `redis` config goes under the same `spring.data` namespace. Let me restructure to avoid YAML key collisions. The `spring.data` already exists at line 9-12. I'll add the Redis config right after the `web` block under `data`:

After line 12 (`max-page-size: 100`), add:

```yaml
  cache:
    type: ${LEDGERLY_CACHE_TYPE:simple}
```

And after line 19 (`driver-class-name: org.postgresql.Driver`), add:

```yaml
    redis:
      host: ${LEDGERLY_REDIS_HOST:localhost}
      port: ${LEDGERLY_REDIS_PORT:6379}
```

This places `redis` under the existing `spring.data` key alongside `web`.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "feat: add cache and redis configuration"
```

---

### Task 4: Disable caching in test application.yml

**Files:**
- Modify: `src/test/resources/application.yml`

- [ ] **Step 1: Add `spring.cache.type: none`**

Insert after `spring.data.web.pageable.max-page-size: 100` (line 11) and before `spring.datasource:` (line 13):

```yaml
  cache:
    type: none
```

- [ ] **Step 2: Commit**

```bash
git add src/test/resources/application.yml
git commit -m "test: disable caching in test profile"
```

---

### Task 5: Create CacheConfig

**Files:**
- Create: `src/main/java/com/ledgerly/config/CacheConfig.java`

- [ ] **Step 1: Create the file**

```java
package com.ledgerly.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
            .withCacheConfiguration("customers",
                RedisCacheConfiguration.defaultCacheConfig()
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer())))
            .withCacheConfiguration("reports",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer())));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/config/CacheConfig.java
git commit -m "feat: add caching configuration with Redis support"
```

---

### Task 6: Add @Cacheable to CustomerLookupImpl

**Files:**
- Modify: `src/main/java/com/ledgerly/customer/internal/CustomerLookupImpl.java`

- [ ] **Step 1: Add @Cacheable annotations**

Add the import and annotations:

The file currently reads:

```java
package com.ledgerly.customer.internal;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerLookup;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class CustomerLookupImpl implements CustomerLookup {

    private final CustomerRepository customerRepository;

    CustomerLookupImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<String> findEmailById(UUID customerId) {
        return customerRepository.findById(customerId).map(Customer::getEmail);
    }

    @Override
    public Optional<CustomerInfo> findInfoById(UUID customerId) {
        return customerRepository.findById(customerId)
            .map(c -> new CustomerInfo(c.getId(), c.getEmail(), c.getName(), c.getPreferredLanguage()));
    }

    @Override
    public boolean exists(UUID customerId) {
        return customerRepository.existsById(customerId);
    }
}
```

Replace with:

```java
package com.ledgerly.customer.internal;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerLookup;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class CustomerLookupImpl implements CustomerLookup {

    private final CustomerRepository customerRepository;

    CustomerLookupImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Cacheable(value = "customers", key = "#customerId")
    public Optional<String> findEmailById(UUID customerId) {
        return customerRepository.findById(customerId).map(Customer::getEmail);
    }

    @Override
    @Cacheable(value = "customers", key = "#customerId")
    public Optional<CustomerInfo> findInfoById(UUID customerId) {
        return customerRepository.findById(customerId)
            .map(c -> new CustomerInfo(c.getId(), c.getEmail(), c.getName(), c.getPreferredLanguage()));
    }

    @Override
    @Cacheable(value = "customers", key = "#customerId")
    public boolean exists(UUID customerId) {
        return customerRepository.existsById(customerId);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/customer/internal/CustomerLookupImpl.java
git commit -m "feat: add cacheable annotations to customer lookups"
```

---

### Task 7: Add @CacheEvict to CustomerService

**Files:**
- Modify: `src/main/java/com/ledgerly/customer/CustomerService.java`

- [ ] **Step 1: Add @CacheEvict annotations and import**

Current imports (lines 1-12):

```java
package com.ledgerly.customer;

import com.ledgerly.customer.internal.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
```

Add the `CacheEvict` import after the existing Spring imports:

```java
package com.ledgerly.customer;

import com.ledgerly.customer.internal.CustomerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
```

Add `@CacheEvict` to the `createCustomer` method (line 29):

```java
    @CacheEvict(value = "customers", key = "#result.id")
    public Customer createCustomer(String name, String email, String taxId, String address) {
```

Add `@CacheEvict` to the `updateCustomer` method (line 38):

```java
    @CacheEvict(value = "customers", key = "#id")
    public Customer updateCustomer(UUID id, String name, String email, String taxId,
                                    String address, String preferredLanguage) {
```

Add `@CacheEvict` to the `deleteCustomer` method (line 75):

```java
    @CacheEvict(value = "customers")
    public void deleteCustomer(UUID id) {
```

Note for `deleteCustomer`: `key = "#id"` won't work because `allEntries = true` or no key is needed for a simple eviction after the method call. Use `allEntries = false` and `key = "#id"`:

Actually, for `deleteCustomer`, evict by key is correct since we know the ID:

```java
    @CacheEvict(value = "customers", key = "#id")
    public void deleteCustomer(UUID id) {
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/customer/CustomerService.java
git commit -m "feat: add cache eviction on customer mutations"
```

---

### Task 8: Add @Cacheable to ReportService

**Files:**
- Modify: `src/main/java/com/ledgerly/reporting/ReportService.java`

- [ ] **Step 1: Add @Cacheable annotations and import**

Current imports (lines 1-7):

```java
package com.ledgerly.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
```

Add the `Cacheable` import:

```java
package com.ledgerly.reporting;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
```

Add annotations to the four summary methods:

**`overallSummary(LocalDate, LocalDate)` (line 36):**

```java
    @Cacheable(value = "reports", key = "'summary-' + (#from != null ? #from : 'all') + '-' + (#to != null ? #to : 'all')")
    public OverallSummary overallSummary(LocalDate from, LocalDate to) {
```

**`customerSummary` (line 41):**

```java
    @Cacheable(value = "reports", key = "'cust-summary-' + #customerId")
    public CustomerSummary customerSummary(UUID customerId) {
```

**`agingReport` (line 45):**

```java
    @Cacheable(value = "reports", key = "'aging'")
    public AgingReport agingReport() {
```

**`agingReportForCustomer` (line 49):**

```java
    @Cacheable(value = "reports", key = "'aging-cust-' + #customerId")
    public AgingReport agingReportForCustomer(UUID customerId) {
```

The `overallSummary()` no-arg overload (line 32) does NOT need `@Cacheable` — it delegates to the two-arg version which IS cached.

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ledgerly/reporting/ReportService.java
git commit -m "feat: add cacheable annotations to report summaries"
```

---

### Task 9: Write CachingIntegrationTest

**Files:**
- Create: `src/test/java/com/ledgerly/CachingIntegrationTest.java`

- [ ] **Step 1: Create the test file**

```java
package com.ledgerly;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.reporting.AgingReport;
import com.ledgerly.reporting.CustomerSummary;
import com.ledgerly.reporting.OverallSummary;
import com.ledgerly.reporting.ReportService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CachingIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer(
        RedisContainer.DEFAULT_IMAGE_NAME.withTag("7-alpine"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldCacheCustomerLookupAfterFirstAccess() {
        Customer customer = customerService.createCustomer(
            "CacheTest Corp", "cache@example.com", "VAT-001", "123 Cache St");

        Cache customers = cacheManager.getCache("customers");
        assertThat(customers).isNotNull();

        assertThat(customers.get(customer.getId())).isNull();

        customerService.findById(customer.getId());

        assertThat(customers.get(customer.getId())).isNotNull();
    }

    @Test
    void shouldEvictCustomerCacheOnUpdate() {
        Customer customer = customerService.createCustomer(
            "EvictTest Corp", "evict@example.com", "VAT-002", "456 Evict St");

        customerService.findById(customer.getId());

        Cache customers = cacheManager.getCache("customers");
        assertThat(customers.get(customer.getId())).isNotNull();

        customerService.updateCustomer(
            customer.getId(), "EvictTest Renamed", "evict@example.com",
            "VAT-002", "456 Evict St", "en");

        assertThat(customers.get(customer.getId())).isNull();
    }

    @Test
    void shouldCacheReportSummary() {
        OverallSummary summary1 = reportService.overallSummary();
        OverallSummary summary2 = reportService.overallSummary();

        assertThat(summary1).isNotNull();
        assertThat(summary1.totalCustomers()).isEqualTo(summary2.totalCustomers());
    }

    @Test
    void shouldCacheCustomerReport() {
        Customer customer = customerService.createCustomer(
            "ReportCache Corp", "reportcache@example.com", "VAT-003", "789 Report St");

        CustomerSummary summary1 = reportService.customerSummary(customer.getId());
        CustomerSummary summary2 = reportService.customerSummary(customer.getId());

        assertThat(summary1).isNotNull();
        assertThat(summary1.customerId()).isEqualTo(summary2.customerId());
        assertThat(summary1.name()).isEqualTo(summary2.name());
    }

    @Test
    void shouldCacheAgingReport() {
        AgingReport report1 = reportService.agingReport();
        AgingReport report2 = reportService.agingReport();

        assertThat(report1).isNotNull();
        assertThat(report1.buckets()).hasSize(5);
        assertThat(report1.totalOutstanding()).isEqualByComparingTo(report2.totalOutstanding());
    }

    @Test
    void shouldCacheCustomerAgingReport() {
        AgingReport report1 = reportService.agingReportForCustomer(UUID.randomUUID());
        AgingReport report2 = reportService.agingReportForCustomer(UUID.randomUUID());

        assertThat(report1).isNotNull();
        assertThat(report1.buckets()).hasSize(5);
    }
}
```

- [ ] **Step 2: Run the caching integration test**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test -Dtest=CachingIntegrationTest`

Expected: 6 tests pass

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ledgerly/CachingIntegrationTest.java
git commit -m "test: add Redis caching integration tests"
```

---

### Task 10: Run full test suite and verify

**Files:**
- None (verification only)

- [ ] **Step 1: Run full test suite**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp test`

Expected: All tests pass (101 existing + 6 new = 107 tests, 0 failures)

Note: The test `application.yml` sets `spring.cache.type: none`, so existing module tests are unaffected.

- [ ] **Step 2: Build the jar**

Run: `docker run --rm -v ${PWD}:/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -B -ntp clean package`

Expected: BUILD SUCCESS

- [ ] **Step 3: Mark Phase 9b complete in IMPLEMENTATION_STATUS.md**

Update line 236 in `docs/IMPLEMENTATION_STATUS.md` from `#### Phase 9b — Redis caching (NOT STARTED)` to `#### Phase 9b — Redis caching (COMPLETED)`, and add a summary of what was delivered below it, following the pattern of Phase 9a.

```markdown
#### Phase 9b — Redis caching (COMPLETED)

**Status:** all planned features implemented.

What was delivered:
- `spring-boot-starter-data-redis` dependency for Redis connectivity and
  `RedisCacheManager` auto-configuration
- `CacheConfig` (`@EnableCaching`) with per-region TTLs: `customers` cache
  (no TTL, eviction-based) and `reports` cache (5-min TTL)
- `CustomerLookupImpl` methods (`findEmailById`, `findInfoById`, `exists`)
  annotated with `@Cacheable`
- `CustomerService` mutations (`createCustomer`, `updateCustomer`,
  `deleteCustomer`) annotated with `@CacheEvict`
- `ReportService` summary methods (`overallSummary`, `customerSummary`,
  `agingReport`, `agingReportForCustomer`) annotated with `@Cacheable`
- `application.yml`: default `spring.cache.type=simple` (in-memory) for
  dev; prod overrides to `redis` via docker compose environment
- `docker-compose.yml`: Redis 7 Alpine service, app service wired to it
- Test `application.yml`: `spring.cache.type=none` to keep existing
  module tests isolated
- `CachingIntegrationTest`: 6 tests using Testcontainers Redis verifying
  cache population, eviction, and report caching
- `testcontainers-redis` 2.2.2 test dependency
```

- [ ] **Step 4: Commit**

```bash
git add docs/IMPLEMENTATION_STATUS.md
git commit -m "docs: mark Phase 9b as completed"
```
