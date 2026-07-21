package com.ledgerly.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer aggregate root. Public type of the {@code customer} module — it is part
 * of the module's published API and may be referenced by other modules.
 *
 * <p>References from other aggregates to a customer are by {@link UUID} id only, so
 * the {@code invoice} and {@code payment} modules do not need to import this entity.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String taxId;

    private String address;

    @Column(nullable = false)
    private String preferredLanguage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Customer() {
        // for JPA
    }

    public Customer(String name, String email, String taxId, String address) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.taxId = taxId;
        this.address = address;
        this.preferredLanguage = "en";
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String email, String taxId, String address, String preferredLanguage) {
        this.name = name;
        this.email = email;
        this.taxId = taxId;
        this.address = address;
        if (preferredLanguage != null && !preferredLanguage.isBlank()) {
            this.preferredLanguage = preferredLanguage;
        }
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getTaxId() { return taxId; }
    public String getAddress() { return address; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}