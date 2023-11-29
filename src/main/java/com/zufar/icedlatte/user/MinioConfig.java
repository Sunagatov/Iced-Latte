package com.zufar.icedlatte.user;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private String endpoint = "http://iced-latte-minio:9000"; // Your MinIO endpoint
    private String region = "us-west-1"; // The region can be a dummy one for MinIO
    private String accessKey = "minio"; // Your access key
    private String secretKey = "minio123"; // Your secret key

    @Bean
    public AmazonS3 amazonS3Client(AWSStaticCredentialsProvider awsStaticCredentialsProvider) {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withPathStyleAccessEnabled(true)
                .withCredentials(awsStaticCredentialsProvider)
                .build();
    }

    @Bean
    public AWSStaticCredentialsProvider getCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }
}

