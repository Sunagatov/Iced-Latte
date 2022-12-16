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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

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
    public DynamoDbEnhancedClient getDynamoDbEnhancedClient() {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(getDynamoDbClient())
                .build();
    }

    @Bean
    public DynamoDbClient getDynamoDbClient() {
        AwsCredentials awsBasicCredentials = AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(awsBasicCredentials);

        return DynamoDbClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.EU_WEST_2)
                .build();
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
