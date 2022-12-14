package com.zufarproject.aws.dynamodb.repository;

import com.zufarproject.aws.dynamodb.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Primary
@Repository
@RequiredArgsConstructor
public class CustomerTestCrudRepository implements CustomerCrudRepository {
    private final Map<String, Customer> customers = new HashMap<>();

    @Override
    public void saveCustomer(final Customer customer) {
        customers.put(customer.getCustomerId(), customer);
    }

    @Override
    public Optional<Customer> getCustomerById(final String customerId) {
       return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public void deleteCustomerById(final String customerId) {
        customers.remove(customerId);
    }

    @Override
    public void updateCustomer(final String customerId, final Customer customer) {
        customers.put(customer.getCustomerId(), customer);
    }
}
