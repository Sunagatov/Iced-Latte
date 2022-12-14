package com.zufarproject.aws.dynamodb.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DynamoDbConfiguration {
    private static final String AWS_ACCESS_KEY = System.getProperty("AWS_ACCESS_KEY");
    private static final String AWS_SECRET_KEY = System.getProperty("AWS_SECRET_KEY");
    private static final String AWS_SERVICE_ENDPOINT = System.getProperty("AWS_SERVICE_ENDPOINT");
    private static final String AWS_REGION = System.getProperty("AWS_REGION");

    @Bean
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(getAmazonDynamoDB());
    }

    @Bean
    public AmazonDynamoDB getAmazonDynamoDB() {
        return AmazonDynamoDBClientBuilder
                .standard()
                .withEndpointConfiguration(getEndpointConfiguration())
                .withCredentials(getAwsSystemPropertyCredentialsProvider())
                .build();
    }

    @Bean
    public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(AWS_SERVICE_ENDPOINT, AWS_REGION);
    }

    @Bean
    public AWSCredentials getAWSCredentials() {
        return new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
    }

    @Bean
    public AWSCredentialsProvider getAwsSystemPropertyCredentialsProvider() {
        return new AWSStaticCredentialsProvider(getAWSCredentials());
    }
}
