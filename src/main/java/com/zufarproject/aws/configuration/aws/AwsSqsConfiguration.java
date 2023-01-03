package com.zufarproject.aws.configuration.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@Getter
public class AwsSqsConfiguration {
	private final AWSCredentialsProvider awsCredentialsProvider;

	@Bean
	public AmazonSQS getAmazonSqs() {
		return AmazonSQSClientBuilder.standard()
				.withCredentials(awsCredentialsProvider)
				.withRegion(Regions.DEFAULT_REGION)
				.build();
	}
}
