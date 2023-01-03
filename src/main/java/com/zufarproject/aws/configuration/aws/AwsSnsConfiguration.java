package com.zufarproject.aws.configuration.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Configuration
@RequiredArgsConstructor
public class AwsSnsConfiguration {
	private final AwsConfiguration awsConfiguration;
	private final AWSCredentialsProvider awsCredentialsProvider;

	@Value("${cloud.aws.sns.topic.url}")
	private String snsEndpoint;

	@Value("${cloud.aws.sns.topic.name}")
	private String topicName;

	@Bean
	public AmazonSNS getAmazonSNS() {
		AwsClientBuilder.EndpointConfiguration endpointConfiguration =
				new AwsClientBuilder.EndpointConfiguration(snsEndpoint, awsConfiguration.getAwsRegion());

		return AmazonSNSClientBuilder.standard()
				.withCredentials(awsCredentialsProvider)
				.withEndpointConfiguration(endpointConfiguration)
				.build();
	}

	@Bean
	public NotificationMessagingTemplate notificationMessagingTemplate() {
		MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
		mappingJackson2MessageConverter.setSerializedPayloadClass(String.class);
		mappingJackson2MessageConverter.getObjectMapper().registerModule(new JavaTimeModule());

		NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(getAmazonSNS());
		notificationMessagingTemplate.setMessageConverter(mappingJackson2MessageConverter);
		notificationMessagingTemplate.setDefaultDestinationName(topicName);
		return notificationMessagingTemplate;
	}
}
