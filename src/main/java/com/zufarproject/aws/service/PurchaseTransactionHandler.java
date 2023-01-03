package com.zufarproject.aws.service;

import com.zufarproject.aws.converter.PurchaseTransactionDtoConverter;
import com.zufarproject.aws.dto.PurchasePurchaseRequest;
import com.zufarproject.aws.dto.PurchaseTransactionDto;
import com.zufarproject.aws.sqs.SqsMessageReceiver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.util.Collection;

import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTransactionHandler {
	private final PurchaseTransactionDtoConverter purchaseTransactionConverter;
	private final NotificationMessagingTemplate notificationMessagingTemplate;
	private final SqsMessageReceiver sqsMessageReceiver;

	@Value("${cloud.aws.sqs.queue.purchase-transactions-sqs-queue-url}")
	public String queueUrl;

	@Value("${cloud.aws.sns.topic.name}")
	private String topicName;

	public void processRequest(final PurchasePurchaseRequest request) {
		log.info("Received request {}.", request);
		PurchaseTransactionDto purchaseTransactionDto = purchaseTransactionConverter.convert(request);

		log.info("Sending purchase transaction {}", purchaseTransactionDto);
		Message<PurchaseTransactionDto> message = new GenericMessage<>(purchaseTransactionDto);
		notificationMessagingTemplate.send(topicName, message);

		log.info("Purchase transaction was sent {}", purchaseTransactionDto);
	}

	public Collection<String> getAllTransactions() {
		return sqsMessageReceiver.receiveAllMessageBodies(queueUrl);
	}
}
