package com.zufar.icedlatte.user.service;

import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadCompletionQueueProperties;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@DisplayName("AvatarUploadCompletionQueuePoller unit tests")
class AvatarUploadCompletionQueuePollerTest {

    @Test
    @DisplayName("handles ready message and deletes it from SQS")
    void pollHandlesReadyMessageAndDeletesIt() {
        SqsClient sqsClient = mock(SqsClient.class);
        AvatarUploadCompletionQueueMessageParser parser = mock(AvatarUploadCompletionQueueMessageParser.class);
        AvatarUploadCompletionHandler handler = mock(AvatarUploadCompletionHandler.class);
        AvatarUploadCompletionCommand command = mock(AvatarUploadCompletionCommand.class);
        AvatarUploadCompletionQueueMessage queueMessage = AvatarUploadCompletionQueueMessage.ready(command);
        Message message = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body("{\"status\":\"READY\"}")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(parser.parse(message.body())).thenReturn(queueMessage);
        when(handler.complete(command)).thenReturn(Optional.empty());

        poller(sqsClient, parser, handler).poll();

        verify(handler).complete(command);
        verify(sqsClient).deleteMessage(deleteRequest());
    }

    @Test
    @DisplayName("handles failure message and deletes it from SQS")
    void pollHandlesFailureMessageAndDeletesIt() {
        SqsClient sqsClient = mock(SqsClient.class);
        AvatarUploadCompletionQueueMessageParser parser = mock(AvatarUploadCompletionQueueMessageParser.class);
        AvatarUploadCompletionHandler handler = mock(AvatarUploadCompletionHandler.class);
        AvatarUploadFailureCommand command = mock(AvatarUploadFailureCommand.class);
        AvatarUploadCompletionQueueMessage queueMessage = AvatarUploadCompletionQueueMessage.failed(command);
        Message message = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body("{\"status\":\"FAILED\"}")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(parser.parse(message.body())).thenReturn(queueMessage);
        when(handler.fail(command)).thenReturn(Optional.empty());

        poller(sqsClient, parser, handler).poll();

        verify(handler).fail(command);
        verify(sqsClient).deleteMessage(deleteRequest());
    }

    @Test
    @DisplayName("keeps message in SQS when handling throws transient exception")
    void pollKeepsMessageWhenHandlerThrowsTransientException() {
        SqsClient sqsClient = mock(SqsClient.class);
        AvatarUploadCompletionQueueMessageParser parser = mock(AvatarUploadCompletionQueueMessageParser.class);
        AvatarUploadCompletionHandler handler = mock(AvatarUploadCompletionHandler.class);
        AvatarUploadCompletionCommand command = mock(AvatarUploadCompletionCommand.class);
        AvatarUploadCompletionQueueMessage queueMessage = AvatarUploadCompletionQueueMessage.ready(command);
        Message message = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body("{\"status\":\"READY\"}")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(parser.parse(message.body())).thenReturn(queueMessage);
        doThrow(new IllegalStateException("database unavailable")).when(handler).complete(command);

        poller(sqsClient, parser, handler).poll();

        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("deletes invalid message from SQS so poison payload does not loop forever")
    void pollDeletesInvalidMessage() {
        SqsClient sqsClient = mock(SqsClient.class);
        AvatarUploadCompletionQueueMessageParser parser = mock(AvatarUploadCompletionQueueMessageParser.class);
        AvatarUploadCompletionHandler handler = mock(AvatarUploadCompletionHandler.class);
        Message message = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body("{\"status\":\"BROKEN\"}")
                .build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(message).build());
        when(parser.parse(message.body()))
                .thenThrow(new BadRequestException("Avatar upload completion message version is unsupported."));

        poller(sqsClient, parser, handler).poll();

        verifyNoInteractions(handler);
        verify(sqsClient).deleteMessage(deleteRequest());
    }

    @Test
    @DisplayName("does not poll SQS when queue URL is blank")
    void pollSkipsBlankQueueUrl() {
        SqsClient sqsClient = mock(SqsClient.class);
        AvatarUploadCompletionQueuePoller poller = new AvatarUploadCompletionQueuePoller(
                sqsClient,
                mock(AvatarUploadCompletionQueueMessageParser.class),
                mock(AvatarUploadCompletionHandler.class),
                uploadProperties(""),
                queueProperties());

        poller.poll();

        verifyNoInteractions(sqsClient);
    }

    private static AvatarUploadCompletionQueuePoller poller(
            SqsClient sqsClient,
            AvatarUploadCompletionQueueMessageParser parser,
            AvatarUploadCompletionHandler handler) {
        return new AvatarUploadCompletionQueuePoller(
                sqsClient, parser, handler, uploadProperties("https://sqs.example.test/q"), queueProperties());
    }

    private static DeleteMessageRequest deleteRequest() {
        return DeleteMessageRequest.builder()
                .queueUrl("https://sqs.example.test/q")
                .receiptHandle("receipt-1")
                .build();
    }

    private static AvatarUploadProperties uploadProperties(String queueUrl) {
        return new AvatarUploadProperties(
                com.zufar.icedlatte.user.config.AvatarUploadMode.PRESIGNED,
                Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                Duration.ofMinutes(10),
                "iced-latte-users",
                "iced-latte-users",
                queueUrl);
    }

    private static AvatarUploadCompletionQueueProperties queueProperties() {
        return new AvatarUploadCompletionQueueProperties(true, 5, Duration.ofSeconds(30), Duration.ofSeconds(1));
    }
}
