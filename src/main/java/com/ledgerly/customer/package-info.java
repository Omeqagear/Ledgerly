/**
 * Customer module: owns the customer master data and exposes lookup contracts
 * ({@link com.ledgerly.customer.CustomerAPI} and
 * {@link com.ledgerly.customer.CustomerLookup}) to other modules.
 *
 * <p>This module has no inbound application-module dependencies; it only publishes
 * events implicitly through being referenced via UUID by other modules.
 */
@org.springframework.modulith.ApplicationModule(
    id = "customer",
    displayName = "Customer Management"
)
package com.ledgerly.customer;