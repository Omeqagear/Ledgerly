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