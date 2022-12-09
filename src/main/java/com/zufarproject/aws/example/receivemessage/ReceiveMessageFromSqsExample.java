package com.zufarproject.aws.example.receivemessage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;

import java.util.List;

public class ReceiveMessageFromSqsExample {
    private static final String QUEUE_URL = System.getProperty("AWS_QUEUE_URL");
    private static final String AWS_ACCESS_KEY = System.getProperty("AWS_ACCESS_KEY");
    private static final String AWS_SECRET_KEY = System.getProperty("AWS_SECRET_KEY");
    private static final AWSCredentials CREDENTIALS = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);

    public static void main(String[] args) {
        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(CREDENTIALS))
                .withRegion(Regions.DEFAULT_REGION)
                .build();

        List<Message> messages = sqsClient
                .receiveMessage(QUEUE_URL)
                .getMessages();

        messages.forEach(message -> System.out.println("***************" + message.getBody()));
    }
}
