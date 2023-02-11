package com.zufar.onlinestore.customer.repository.dynamodb.client;

import com.zufar.onlinestore.customer.repository.dynamodb.client.model.CustomerClientEntity;
import com.zufar.onlinestore.customer.repository.dynamodb.CrudRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.Collection;
import java.util.Optional;


@Profile("Aws-Profile")
@Repository
@RequiredArgsConstructor
public class CustomerDynamoDbClient implements CrudRepository<CustomerClientEntity> {
    public static final String CUSTOMER_TABLE_NAME = "Customer";

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Override
    public void save(CustomerClientEntity customer) {
        DynamoDbTable<CustomerClientEntity> customerTable = getCustomerDynamoDbTable();
        customerTable.putItem(customer);
    }

    @Override
    public void save(Collection<CustomerClientEntity> customers) {
        WriteBatch.Builder<CustomerClientEntity> customerBuilder = WriteBatch
                .builder(CustomerClientEntity.class)
                .mappedTableResource(getCustomerDynamoDbTable());

        customers.forEach(customerBuilder::addPutItem);

        WriteBatch writeBatch = customerBuilder.build();

        BatchWriteItemEnhancedRequest batchWriteItemEnhancedRequest = BatchWriteItemEnhancedRequest
                .builder()
                .writeBatches(writeBatch)
                .build();

        dynamoDbEnhancedClient.batchWriteItem(batchWriteItemEnhancedRequest);
    }

    @Override
    public Optional<CustomerClientEntity> getById(String customerId) {
        Key key = Key.builder()
                .partitionValue(customerId)
                .build();

        CustomerClientEntity customer = getCustomerDynamoDbTable().
                getItem((GetItemEnhancedRequest.Builder requestBuilder) -> requestBuilder.key(key));
        return Optional.ofNullable(customer);
    }

	@Override
	public Optional<Collection<CustomerClientEntity>> getAll() {
		return Optional.empty();
	}

	@Override
    public void deleteById(String customerId) {
        Key key = Key.builder()
                .partitionValue(customerId)
                .build();
        getCustomerDynamoDbTable()
                .deleteItem(key);
    }

    @Override
    public void deleteByIds(Collection<String> customerIds) {
        WriteBatch.Builder<CustomerClientEntity> customerBuilder = WriteBatch
                .builder(CustomerClientEntity.class)
                .mappedTableResource(getCustomerDynamoDbTable());

        customerIds.stream()
                .map(customerId -> DeleteItemEnhancedRequest
                        .builder()
                        .key(Key.builder()
                                .partitionValue(customerId)
                                .build())
                        .build())
                .forEach(customerBuilder::addDeleteItem);

        WriteBatch writeBatch = customerBuilder.build();

        BatchWriteItemEnhancedRequest request = BatchWriteItemEnhancedRequest.builder()
                .writeBatches(writeBatch)
                .build();

        dynamoDbEnhancedClient.batchWriteItem(request);
    }

    @Override
    public void update(String customerId, CustomerClientEntity customer) {
        customer.setCustomerId(customerId);
        getCustomerDynamoDbTable().updateItem(customer);
    }

    private DynamoDbTable<CustomerClientEntity> getCustomerDynamoDbTable() {
        return dynamoDbEnhancedClient.table(CUSTOMER_TABLE_NAME, TableSchema.fromBean(CustomerClientEntity.class));
    }
}
