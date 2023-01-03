package com.zufarproject.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.zufarproject.aws.configuration.aws.AwsSqsConfiguration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;


@Service
@RequiredArgsConstructor
public class SqsMessageReceiver {
    private final AwsSqsConfiguration awsSqsConfiguration;
    private final AmazonSQS sqsClient;

    public Collection<Message> receive() {
        return sqsClient
                .receiveMessage(awsSqsConfiguration.getQueueUrl())
                .getMessages();
    }
}
