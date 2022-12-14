package com.zufarproject.aws.dynamodb.repository;

import com.zufarproject.aws.dynamodb.model.Customer;

public interface CustomerCrudRepository {

    Customer saveCustomer(final Customer customer);

    Customer getCustomerById(final String customerId);

    void deleteCustomerById(final String customerId);

    void updateCustomer(final String customerId, final Customer customer);
}
