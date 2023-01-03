package com.zufarproject.aws.service;

import com.zufarproject.aws.configuration.aws.AwsSnsConfiguration;
import com.zufarproject.aws.converter.PurchaseTransactionDtoConverter;
import com.zufarproject.aws.dto.PurchaseProductRequest;
import com.zufarproject.aws.dto.PurchaseTransactionDto;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTransactionHandler {
	private final PurchaseTransactionDtoConverter purchaseTransactionConverter;
	private final NotificationMessagingTemplate notificationMessagingTemplate;
	private final AwsSnsConfiguration awsSnsConfiguration;

	public void processRequest(final PurchaseProductRequest request) {
		log.info("Received request {}.", request);
		PurchaseTransactionDto purchaseTransactionDto = purchaseTransactionConverter.convert(request);

		log.info("Sending purchase transaction {}", purchaseTransactionDto);
		Message<PurchaseTransactionDto> message = new GenericMessage<>(purchaseTransactionDto);
		notificationMessagingTemplate.send(awsSnsConfiguration.getTopicName(), message);
		log.info("Purchase transaction was sent {}", purchaseTransactionDto);
	}
}
