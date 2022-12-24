package com.zufarproject.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class SqsBatchMessageSender {
    private static final String QUEUE_URL = System.getProperty("AWS_QUEUE_URL");

    private final AmazonSQS sqsClient;

    public void send(Collection<SendMessageBatchRequestEntry> messages) {
        final SendMessageBatchRequest messageBatchRequest = new SendMessageBatchRequest()
                .withQueueUrl(QUEUE_URL)
                .withEntries(messages);

        sqsClient.sendMessageBatch(messageBatchRequest);
    }
}
