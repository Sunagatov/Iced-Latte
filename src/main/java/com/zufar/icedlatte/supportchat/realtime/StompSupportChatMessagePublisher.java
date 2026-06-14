package com.zufar.icedlatte.supportchat.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zufar.icedlatte.openapi.dto.SupportChatMessageDto;
import com.zufar.icedlatte.supportchat.converter.SupportChatDtoConverter;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSupportChatMessagePublisher implements SupportChatMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final SupportChatDtoConverter dtoConverter;

    @Override
    public void publishOwnerReply(SupportConversationEntity conversation, SupportMessageEntity message) {
        String destination = SupportChatWebSocketDestinations.conversationMessages(conversation.getId());
        SupportChatMessageDto payload = dtoConverter.toMessageDto(message);
        if (isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(destination, payload, conversation, message);
                }
            });
            return;
        }
        send(destination, payload, conversation, message);
    }

    private void send(
            String destination,
            SupportChatMessageDto payload,
            SupportConversationEntity conversation,
            SupportMessageEntity message) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (RuntimeException ex) {
            log.warn(
                    "support_chat.websocket.owner_reply.publish_failed: conversationId={}, messageId={}, exceptionClass={}",
                    conversation.getId(),
                    message.getId(),
                    ex.getClass().getSimpleName());
        }
    }
}
