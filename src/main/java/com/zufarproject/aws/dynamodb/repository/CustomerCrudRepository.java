package com.zufarproject.aws.dynamodb.repository;

import com.zufarproject.aws.dynamodb.model.Customer;

import java.util.Collection;
import java.util.Optional;

public interface CustomerCrudRepository {

    void saveCustomer(final Customer customer);

    void saveCustomers(final Collection<Customer> customers);

    Optional<Customer> getCustomerById(final String customerId);

    void deleteCustomerById(final String customerId);

    void deleteCustomersByIds(Collection<String> customerIds);

    void updateCustomer(final String customerId, final Customer customer);
}
