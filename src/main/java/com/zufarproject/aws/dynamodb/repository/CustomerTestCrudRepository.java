package com.zufarproject.aws.dynamodb.repository;

import com.zufarproject.aws.dynamodb.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Primary
@Repository
@RequiredArgsConstructor
public class CustomerTestCrudRepository implements CustomerCrudRepository {
    private final Map<String, Customer> customers = new HashMap<>();

    @Override
    public Customer saveCustomer(final Customer customer) {
        customers.put(customer.getCustomerId(), customer);
        return customer;
    }

    @Override
    public Customer getCustomerById(final String customerId) {
        return customers.get(customerId);
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
