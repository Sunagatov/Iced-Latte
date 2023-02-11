package com.zufar.onlinestore.transacton;

import com.zufar.onlinestore.transacton.converter.PurchaseTransactionDtoConverter;
import com.zufar.onlinestore.transacton.dto.TransactionRequest;
import com.zufar.onlinestore.transacton.dto.PurchaseTransactionDto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTransactionHandler {
	private final PurchaseTransactionDtoConverter purchaseTransactionConverter;

	@Value("${cloud.aws.sqs.queue.purchase-transactions-sqs-queue-url}")
	public String queueUrl;

	@Value("${cloud.aws.sns.topic.name}")
	private String topicName;

	public void processRequest(final TransactionRequest request) {
		log.info("Received request {}.", request);
		PurchaseTransactionDto purchaseTransactionDto = purchaseTransactionConverter.convert(request);

		log.info("Sending purchase transaction {}", purchaseTransactionDto);
		Message<PurchaseTransactionDto> message = new GenericMessage<>(purchaseTransactionDto);

		log.info("Purchase transaction was sent {}", purchaseTransactionDto);
	}

	public Collection<String> getAllTransactions() {
		return Collections.emptyList();
	}
}
