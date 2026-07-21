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