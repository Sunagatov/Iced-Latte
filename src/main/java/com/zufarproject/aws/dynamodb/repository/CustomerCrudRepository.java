package com.zufarproject.aws.dynamodb.repository;

import com.zufarproject.aws.dynamodb.model.Customer;

import java.util.Optional;

public interface CustomerCrudRepository {

    void saveCustomer(final Customer customer);

    Optional<Customer> getCustomerById(final String customerId);

    void deleteCustomerById(final String customerId);

    void updateCustomer(final String customerId, final Customer customer);
}
