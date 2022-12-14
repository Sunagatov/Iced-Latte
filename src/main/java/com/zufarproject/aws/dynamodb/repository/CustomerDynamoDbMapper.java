package com.zufarproject.aws.dynamodb.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.zufarproject.aws.dynamodb.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public Optional<Customer> getCustomerById(final String customerId) {
        return Optional.ofNullable(dynamoDBMapper.load(Customer.class, customerId));
    }

    @Override
    public void deleteCustomerById(final String customerId) {
        dynamoDBMapper.delete(dynamoDBMapper.load(Customer.class, customerId));
    }

    @Override
    public void updateCustomer(final String customerId, final Customer customer) {
        AttributeValue attributeValue = new AttributeValue().withS(customerId);
        ExpectedAttributeValue expectedAttributeValue = new ExpectedAttributeValue(attributeValue);
        String attributeName = "customerId";

        DynamoDBSaveExpression dynamoDBSaveExpression = new DynamoDBSaveExpression()
                .withExpectedEntry(
                        attributeName,
                        expectedAttributeValue
                );
        dynamoDBMapper.save(customer, dynamoDBSaveExpression);
    }
}
