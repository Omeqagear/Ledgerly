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

/**
 * Public application service for {@link Customer} aggregates. Implements
 * {@link CustomerAPI} so other modules can depend on the interface, while
 * HTTP and write-side operations live on the concrete class.
 */
@Service
@Transactional
public class CustomerService implements CustomerAPI {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(String name, String email, String taxId, String address) {
        assertEmailNotInUse(email);
        try {
            return customerRepository.save(new Customer(name, email, taxId, address));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCustomerEmailException(email);
        }
    }

    public Customer updateCustomer(UUID id, String name, String email, String taxId,
                                    String address, String preferredLanguage) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException(id));
        if (!email.equalsIgnoreCase(customer.getEmail())) {
            assertEmailNotInUse(email);
        }
        customer.update(name, email, taxId, address, preferredLanguage);
        try {
            return customerRepository.save(customer);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateCustomerEmailException(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findById(UUID id) {
        return customerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return customerRepository.existsById(id);
    }

    public void deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        customerRepository.deleteById(id);
    }

    private void assertEmailNotInUse(String email) {
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateCustomerEmailException(email);
        }
    }
}
