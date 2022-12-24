package com.zufarproject.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class SqsMessageReceiver {
    private static final String QUEUE_URL = System.getProperty("AWS_QUEUE_URL");

    private final AmazonSQS sqsClient;

    public Collection<Message> receive() {
        return sqsClient
                .receiveMessage(QUEUE_URL)
                .getMessages();
    }
}
