package com.zufar.onlinestore.aws.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Profile("Aws-Profile")
@Service
@RequiredArgsConstructor
public class SqsMessageReceiver {
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
