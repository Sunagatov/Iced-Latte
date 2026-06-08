package com.zufar.icedlatte.supportchat.telegram;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageDeliveryResult;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "support-chat.owner-message-mode", havingValue = "TELEGRAM")
class TelegramOwnerMessageSender implements OwnerMessageSender {

    private final SupportChatProperties properties;
    private final SupportConversationRepository conversationRepository;
    private final TelegramBotClient telegramBotClient;
    private final TelegramOwnerMessageFormatter formatter;

    TelegramOwnerMessageSender(
            SupportChatProperties properties,
            SupportConversationRepository conversationRepository,
            TelegramBotClient telegramBotClient,
            TelegramOwnerMessageFormatter formatter) {
        this.properties = properties;
        this.conversationRepository = conversationRepository;
        this.telegramBotClient = telegramBotClient;
        this.formatter = formatter;
    }

    @Override
    @Transactional
    public OwnerMessageDeliveryResult send(OwnerMessage message) {
        return conversationRepository
                .findById(message.conversationId())
                .map(conversation -> send(message, conversation))
                .orElseGet(() -> {
                    log.warn(
                            "support_chat.telegram.conversation_not_found: conversationId={}",
                            message.conversationId());
                    return OwnerMessageDeliveryResult.failedResult();
                });
    }

    private OwnerMessageDeliveryResult send(OwnerMessage message, SupportConversationEntity conversation) {
        String text = formatter.format(message);
        Long threadId = conversation.getTelegramMessageThreadId();
        if (threadId != null) {
            OwnerMessageDeliveryResult topicDelivery = sendToExistingTopic(threadId, text);
            if (topicDelivery.delivered()) {
                return topicDelivery;
            }
            return sendFallback(conversation, text);
        }
        if (properties.telegram().forumTopicsEnabled()) {
            Optional<TelegramForumTopic> topic = telegramBotClient.createForumTopic(formatter.topicName(message));
            if (topic.isPresent()) {
                conversation.setTelegramMessageThreadId(topic.get().messageThreadId());
                OwnerMessageDeliveryResult topicDelivery =
                        sendToExistingTopic(topic.get().messageThreadId(), text);
                if (topicDelivery.delivered()) {
                    return topicDelivery;
                }
                return sendFallback(conversation, text);
            }
        }
        return sendFallback(conversation, text);
    }

    private OwnerMessageDeliveryResult sendToExistingTopic(Long threadId, String text) {
        if (telegramBotClient.sendMessage(threadId, text).isPresent()) {
            return OwnerMessageDeliveryResult.deliveredResult();
        }
        return OwnerMessageDeliveryResult.failedResult();
    }

    private OwnerMessageDeliveryResult sendFallback(SupportConversationEntity conversation, String text) {
        Optional<TelegramMessageRef> sent = telegramBotClient.sendMessage(null, text);
        if (sent.isEmpty()) {
            return OwnerMessageDeliveryResult.failedResult();
        }
        if (conversation.getTelegramFallbackMessageId() == null) {
            conversation.setTelegramFallbackMessageId(sent.get().messageId());
        }
        return OwnerMessageDeliveryResult.deliveredResult();
    }
}
