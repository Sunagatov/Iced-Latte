package com.zufar.icedlatte.supportchat.telegram;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageDeliveryResult;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "support-chat.owner-message-mode", havingValue = "TELEGRAM")
@RequiredArgsConstructor
class TelegramOwnerMessageSender implements OwnerMessageSender {

    private final SupportChatProperties properties;
    private final SupportConversationRepository conversationRepository;
    private final TelegramBotClient telegramBotClient;
    private final TelegramOwnerMessageFormatter formatter;

    @Override
    public OwnerMessageDeliveryResult send(OwnerMessage message) {
        UUID conversationId = message.conversationId();
        return conversationRepository
                .findById(conversationId)
                .map(conversation -> send(message, conversation))
                .orElseGet(() -> {
                    String logMessage = "support_chat.telegram.conversation_not_found: conversationId={}";
                    log.warn(logMessage, conversationId);
                    return OwnerMessageDeliveryResult.failedResult();
                });
    }

    private OwnerMessageDeliveryResult send(OwnerMessage message, SupportConversationEntity conversation) {
        String text = formatter.format(message);
        Long threadId = conversation.getTelegramMessageThreadId();
        if (threadId != null) {
            OwnerMessageDeliveryResult topicDelivery = sendToExistingTopic(threadId, text);
            boolean delivered = topicDelivery.delivered();
            if (delivered) {
                return topicDelivery;
            }
            return sendFallback(conversation, text);
        }
        if (properties.telegram().forumTopicsEnabled()) {
            String topicName = formatter.topicName(message);
            Optional<TelegramForumTopic> topic = telegramBotClient.createForumTopic(topicName);
            if (topic.isPresent()) {
                long telegramMessageThreadId = topic.get().messageThreadId();
                conversation.setTelegramMessageThreadId(telegramMessageThreadId);
                conversationRepository.save(conversation);
                OwnerMessageDeliveryResult topicDelivery = sendToExistingTopic(telegramMessageThreadId, text);
                if (!topicDelivery.delivered()) {
                    return sendFallback(conversation, text);
                }
                return topicDelivery;
            }
        }
        return sendFallback(conversation, text);
    }

    private OwnerMessageDeliveryResult sendToExistingTopic(Long threadId, String text) {
        Optional<TelegramMessageRef> sent = telegramBotClient.sendMessage(threadId, text);
        if (sent.isPresent()) {
            return OwnerMessageDeliveryResult.deliveredResult();
        }
        return OwnerMessageDeliveryResult.failedResult();
    }

    private OwnerMessageDeliveryResult sendFallback(SupportConversationEntity conversation, String text) {
        Optional<TelegramMessageRef> sent = telegramBotClient.sendMessage(null, text);
        if (sent.isEmpty()) {
            return OwnerMessageDeliveryResult.failedResult();
        }
        conversation.setTelegramFallbackMessageId(sent.get().messageId());
        conversationRepository.save(conversation);
        return OwnerMessageDeliveryResult.deliveredResult();
    }
}
