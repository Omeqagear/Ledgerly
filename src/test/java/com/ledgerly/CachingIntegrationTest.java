package com.ledgerly;

import com.ledgerly.customer.Customer;
import com.ledgerly.customer.CustomerService;
import com.ledgerly.reporting.AgingReport;
import com.ledgerly.reporting.CustomerSummary;
import com.ledgerly.reporting.OverallSummary;
import com.ledgerly.reporting.ReportService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Tag;
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
@Tag("redis")
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
        assertThat(summary1.customerName()).isEqualTo(summary2.customerName());
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
        assertThat(report1).isNotNull();
        assertThat(report1.buckets()).hasSize(5);
    }
}
