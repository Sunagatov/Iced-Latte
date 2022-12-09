package com.zufarproject.aws.example.sendmessage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;

import java.util.Set;

public class SendBatchMessageToSqsQueueExample {
    private static final String QUEUE_URL = System.getProperty("AWS_QUEUE_URL");
    private static final String AWS_ACCESS_KEY = System.getProperty("AWS_ACCESS_KEY");
    private static final String AWS_SECRET_KEY = System.getProperty("AWS_SECRET_KEY");
    private static final AWSCredentials CREDENTIALS = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);

    public static void main(String[] args) {
        final SendMessageBatchRequest messageBatchRequest = new SendMessageBatchRequest()
                .withQueueUrl(QUEUE_URL)
                .withEntries(Set.of(new SendMessageBatchRequestEntry("msg_1", "Hello from message 1"),
                                new SendMessageBatchRequestEntry("msg_2", "Hello from message 2").withDelaySeconds(3)
                ));

        final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(CREDENTIALS))
                .withRegion(Regions.DEFAULT_REGION)
                .build();

        sqsClient.sendMessageBatch(messageBatchRequest);
    }
}
