package com.zufar.onlinestore.customer.repository.dynamodb.mapper;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.zufar.onlinestore.customer.entity.Customer;
import com.zufar.onlinestore.customer.repository.dynamodb.CrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Profile("Aws-Profile")
@Primary
@Repository
@RequiredArgsConstructor
public class CustomerDynamoDbMapper implements CrudRepository<Customer> {
    private final DynamoDBMapper dynamoDBMapper;

    @Override
    public void save(final Customer customer) {
        dynamoDBMapper.save(customer);
    }

    @Override
    public void save(final Collection<Customer> customers) {
        dynamoDBMapper.batchSave(customers);
    }

    @Override
    public Optional<Customer> getById(final String customerId) {
        return Optional.ofNullable(dynamoDBMapper.load(Customer.class, customerId));
    }

	@Override
	public Optional<Collection<Customer>> getAll() {
        return Optional.ofNullable(dynamoDBMapper.scan(Customer.class, new DynamoDBScanExpression()));
	}

	@Override
    public void deleteById(final String customerId) {
        dynamoDBMapper.delete(dynamoDBMapper.load(Customer.class, customerId));
    }

    @Override
    public void deleteByIds(final Collection<String> customerIds) {
        dynamoDBMapper.batchDelete(dynamoDBMapper.load(Customer.class, customerIds));
    }

    @Override
    public void update(final String customerId, final Customer customer) {
        customer.setCustomerId(customerId);

        DynamoDBMapperConfig dynamoDBMapperConfig = new DynamoDBMapperConfig.Builder()
                .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .build();
        dynamoDBMapper.save(customer, dynamoDBMapperConfig);
    }
}
