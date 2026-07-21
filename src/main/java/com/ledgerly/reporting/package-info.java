/**
 * Reporting module: read-only aggregations across customer, invoice and
 * payment data. Implemented with raw SQL through {@link
 * com.ledgerly.reporting.internal.ReportRepository} so the module has no
 * compile-time dependency on other application modules.
 */
@org.springframework.modulith.ApplicationModule(
    id = "reporting",
    displayName = "Reporting"
)
package com.ledgerly.reporting;