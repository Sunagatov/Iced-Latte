package com.zufarproject.aws.dynamodb.repository.dynamodb.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class CustomerClientEntity {
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private AddressClientEntity address;

    @DynamoDbPartitionKey
    public String getCustomerId() {
        return customerId;
    }
}
