package com.zufar.icedlatte.supportchat.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zufar.icedlatte.common.monitoring.SentryHandledExceptionReporter;
import com.zufar.icedlatte.openapi.dto.SupportChatMessageDto;
import com.zufar.icedlatte.supportchat.converter.SupportChatDtoConverter;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType;

@DisplayName("StompSupportChatMessagePublisher unit tests")
class StompSupportChatMessagePublisherTest {

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final SentryHandledExceptionReporter sentryHandledExceptionReporter =
            mock(SentryHandledExceptionReporter.class);

    @Test
    @DisplayName("Publishes owner reply to support chat conversation destination")
    void publishOwnerReply_validMessage_sendsMessageDtoToConversationDestination() {
        StompSupportChatMessagePublisher publisher = new StompSupportChatMessagePublisher(
                messagingTemplate, new SupportChatDtoConverter(), sentryHandledExceptionReporter);

        publisher.publishOwnerReply(conversation(), ownerMessage());

        ArgumentCaptor<SupportChatMessageDto> payloadCaptor = ArgumentCaptor.forClass(SupportChatMessageDto.class);
        verify(messagingTemplate)
                .convertAndSend(
                        eq(SupportChatWebSocketDestinations.conversationMessages(CONVERSATION_ID)),
                        payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getId()).isEqualTo(MESSAGE_ID);
        assertThat(payloadCaptor.getValue().getConversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(payloadCaptor.getValue().getSenderType()).isEqualTo(SupportChatMessageDto.SenderTypeEnum.OWNER);
        assertThat(payloadCaptor.getValue().getBody()).isEqualTo("Owner answer");
    }

    @Test
    @DisplayName("Defers owner reply publish until active transaction commits")
    void publishOwnerReply_activeTransaction_sendsAfterCommit() {
        StompSupportChatMessagePublisher publisher = new StompSupportChatMessagePublisher(
                messagingTemplate, new SupportChatDtoConverter(), sentryHandledExceptionReporter);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            publisher.publishOwnerReply(conversation(), ownerMessage());

            verifyNoInteractions(messagingTemplate);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            verify(messagingTemplate)
                    .convertAndSend(
                            eq(SupportChatWebSocketDestinations.conversationMessages(CONVERSATION_ID)),
                            any(SupportChatMessageDto.class));
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("Publisher failure does not leak exception")
    void publishOwnerReply_messagingTemplateThrows_swallowsException() {
        StompSupportChatMessagePublisher publisher = new StompSupportChatMessagePublisher(
                messagingTemplate, new SupportChatDtoConverter(), sentryHandledExceptionReporter);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(messagingTemplate)
                .convertAndSend(anyString(), any(Object.class));

        publisher.publishOwnerReply(conversation(), ownerMessage());

        verify(sentryHandledExceptionReporter).capture(any(IllegalStateException.class), any());
    }

    private static SupportConversationEntity conversation() {
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(CONVERSATION_ID);
        return conversation;
    }

    private static SupportMessageEntity ownerMessage() {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setId(MESSAGE_ID);
        message.setConversationId(CONVERSATION_ID);
        message.setSenderType(SupportMessageSenderType.OWNER);
        message.setBody("Owner answer");
        message.setDeliveryStatus(SupportMessageDeliveryStatus.SENT);
        message.setCreatedAt(OffsetDateTime.now());
        return message;
    }
}
