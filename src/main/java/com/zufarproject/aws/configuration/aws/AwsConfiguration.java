package com.zufarproject.aws.configuration.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Getter
@Configuration
public class AwsConfiguration {

	@Value("${cloud.aws.credentials.access-key}")
	private String awsAccessKey;

	@Value("${cloud.aws.credentials.secret-key}")
	private String awsSecretKey;

	@Value("${cloud.aws.service.endpoint}")
	private String awsServiceEndpoint;

	@Value("${cloud.aws.region.static}")
	private String awsRegion;

	@Bean
	public AWSCredentials getAWSCredentials() {
		return new BasicAWSCredentials(awsAccessKey, awsSecretKey);
	}

	@Bean
	public AWSCredentialsProvider getAwsSystemPropertyCredentialsProvider() {
		return new AWSStaticCredentialsProvider(getAWSCredentials());
	}

	@Bean
	public AwsCredentialsProvider getAwsCredentialsProvider() {
		return StaticCredentialsProvider.create(getAwsBasicCredentials());
	}

	@Bean
	public AwsBasicCredentials getAwsBasicCredentials() {
		return AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
	}

	@Bean
	public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
		return new AwsClientBuilder.EndpointConfiguration(awsServiceEndpoint, awsRegion);
	}
}
