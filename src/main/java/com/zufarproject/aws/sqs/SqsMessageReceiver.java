package com.zufarproject.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.zufarproject.aws.configuration.aws.AwsSqsConfiguration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SqsMessageReceiver {
    private final AwsSqsConfiguration awsSqsConfiguration;
    private final AmazonSQS sqsClient;

    public Collection<String> receiveAllMessageBodies(final String queueUrl) {
        List<Message> messages = sqsClient
                .receiveMessage(queueUrl)
                .getMessages();

        return messages
                .stream()
                .map(Message::getBody)
                .collect(Collectors.toList());
    }
}
