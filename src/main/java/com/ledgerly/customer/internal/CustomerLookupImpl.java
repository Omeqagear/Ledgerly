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