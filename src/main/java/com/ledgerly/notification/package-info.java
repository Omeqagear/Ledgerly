/**
 * Notification module: reacts to domain events emitted by the {@code invoice}
 * and {@code payment} modules and turns them into customer-facing emails. It
 * also depends on the {@code customer} module's {@link
 * com.ledgerly.customer.CustomerLookup} to resolve email addresses.
 */
@org.springframework.modulith.ApplicationModule(
    id = "notification",
    displayName = "Notification",
    allowedDependencies = {"customer", "invoice", "payment"}
)
package com.ledgerly.notification;