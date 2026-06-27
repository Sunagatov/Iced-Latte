package com.zufar.icedlatte.user.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadCompletionQueueProperties;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "avatar.completion-queue.enabled", havingValue = "true")
@ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
@ConditionalOnProperty(name = "avatar.upload-mode", havingValue = "presigned")
public class AvatarUploadCompletionQueuePoller {

    private final SqsClient sqsClient;
    private final AvatarUploadCompletionQueueMessageParser parser;
    private final AvatarUploadCompletionHandler completionHandler;
    private final AvatarUploadProperties uploadProperties;
    private final AvatarUploadCompletionQueueProperties queueProperties;

    @Scheduled(fixedDelayString = "${avatar.completion-queue.poll-interval:PT5S}")
    public void poll() {
        String queueUrl = uploadProperties.completionQueueUrl();
        if (!StringUtils.hasText(queueUrl)) {
            log.warn("avatar.upload_completion_queue.poll_skipped: reason=queue_url_blank");
            return;
        }

        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(queueProperties.maxMessages())
                .visibilityTimeout((int) queueProperties.visibilityTimeout().toSeconds())
                .waitTimeSeconds((int) queueProperties.waitTime().toSeconds())
                .build();
        sqsClient.receiveMessage(request).messages().forEach(message -> handle(queueUrl, message));
    }

    private void handle(String queueUrl, Message message) {
        try {
            AvatarUploadCompletionQueueMessage queueMessage = parser.parse(message.body());
            if (queueMessage.ready()) {
                completionHandler.complete(queueMessage.completionCommand());
            } else {
                completionHandler.fail(queueMessage.failureCommand());
            }
            delete(queueUrl, message);
        } catch (BadRequestException ex) {
            log.warn(
                    "avatar.upload_completion_queue.message_rejected: messageId={}, reason={}",
                    message.messageId(),
                    ex.getMessage());
            delete(queueUrl, message);
        } catch (RuntimeException ex) {
            log.warn(
                    "avatar.upload_completion_queue.message_failed: messageId={}, exceptionClass={}",
                    message.messageId(),
                    ex.getClass().getSimpleName(),
                    ex);
        }
    }

    private void delete(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}
