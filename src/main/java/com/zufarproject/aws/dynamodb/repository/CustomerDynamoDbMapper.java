package com.zufarproject.aws.dynamodb.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.zufarproject.aws.dynamodb.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomerDynamoDbMapper implements CustomerCrudRepository {
    private final DynamoDBMapper dynamoDBMapper;

    @Override
    public void saveCustomer(final Customer customer) {
        dynamoDBMapper.save(customer);
    }

    @Override
    public void saveCustomers(final Collection<Customer> customers) {
        dynamoDBMapper.batchSave(customers);
    }

    @Override
    public Optional<Customer> getCustomerById(final String customerId) {
        return Optional.ofNullable(dynamoDBMapper.load(Customer.class, customerId));
    }

    @Override
    public void deleteCustomerById(final String customerId) {
        dynamoDBMapper.delete(dynamoDBMapper.load(Customer.class, customerId));
    }

    @Override
    public void deleteCustomersByIds(final Collection<String> customerIds) {
        dynamoDBMapper.batchDelete(dynamoDBMapper.load(Customer.class, customerIds));
    }

    @Override
    public void updateCustomer(final String customerId, final Customer customer) {
        customer.setCustomerId(customerId);

        DynamoDBMapperConfig dynamoDBMapperConfig = new DynamoDBMapperConfig.Builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .build();
        dynamoDBMapper.save(customer, dynamoDBMapperConfig);
    }
}
