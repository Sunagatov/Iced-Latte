package com.zufar.icedlatte.supportchat.telegram;

import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.exception.SupportChatException;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.service.SupportChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class TelegramWebhookService {

    private final SupportChatProperties properties;
    private final SupportConversationRepository conversationRepository;
    private final SupportChatService supportChatService;

    TelegramWebhookResult handle(String secretToken, TelegramWebhookUpdate update) {
        if (!isAuthorizedSecret(secretToken)) {
            log.warn("support_chat.telegram.webhook.unauthorized");
            return TelegramWebhookResult.UNAUTHORIZED;
        }
        if (!properties.enabled() || properties.ownerMessageMode() != OwnerMessageMode.TELEGRAM) {
            return TelegramWebhookResult.IGNORED;
        }

        TelegramWebhookUpdate.TelegramWebhookMessage message = update == null ? null : update.message();
        if (!isSupportedOwnerReply(update, message)) {
            logUnsupportedOwnerReply(update, message);
            return TelegramWebhookResult.IGNORED;
        }

        Optional<SupportConversationEntity> conversation = findConversation(message);
        if (conversation.isEmpty()) {
            log.warn("support_chat.telegram.webhook.conversation_not_found: telegramUpdateId={}", update.updateId());
            return TelegramWebhookResult.IGNORED;
        }

        try {
            supportChatService.saveOwnerReply(
                    conversation.get(), message.text(), update.updateId(), message.messageId());
            return TelegramWebhookResult.PROCESSED;
        } catch (SupportChatException | DataIntegrityViolationException ex) {
            log.warn(
                    "support_chat.telegram.webhook.rejected: telegramUpdateId={}, exceptionClass={}",
                    update.updateId(),
                    ex.getClass().getSimpleName());
            return TelegramWebhookResult.IGNORED;
        }
    }

    private boolean isAuthorizedSecret(String secretToken) {
        String expected = properties.telegram().webhookSecret();
        return !expected.isBlank() && Objects.equals(expected, secretToken);
    }

    private boolean isSupportedOwnerReply(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        if (update == null || update.updateId() == null || message == null || message.messageId() == null) {
            return false;
        }
        if (message.text() == null || message.text().isBlank()) {
            return false;
        }
        if (message.chat() == null
                || !Objects.equals(
                        properties.telegram().chatId(),
                        String.valueOf(message.chat().id()))) {
            return false;
        }
        if (message.from() == null
                || !Objects.equals(
                        properties.telegram().ownerUserId(), message.from().id())) {
            return false;
        }
        return !Boolean.TRUE.equals(message.from().bot());
    }

    private void logUnsupportedOwnerReply(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        log.warn(
                "support_chat.telegram.webhook.unsupported_update: telegramUpdateId={}, telegramMessageId={}, reason={}",
                update == null ? null : update.updateId(),
                message == null ? null : message.messageId(),
                unsupportedOwnerReplyReason(update, message));
    }

    private String unsupportedOwnerReplyReason(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        if (update == null || update.updateId() == null) {
            return "missing_update_id";
        }
        if (message == null || message.messageId() == null) {
            return "missing_message";
        }
        if (message.text() == null || message.text().isBlank()) {
            return "non_text_message";
        }
        if (message.chat() == null) {
            return "missing_chat";
        }
        if (!Objects.equals(
                properties.telegram().chatId(), String.valueOf(message.chat().id()))) {
            return "unexpected_chat";
        }
        if (message.from() == null) {
            return "missing_sender";
        }
        if (!Objects.equals(properties.telegram().ownerUserId(), message.from().id())) {
            return "unexpected_sender";
        }
        if (Boolean.TRUE.equals(message.from().bot())) {
            return "bot_sender";
        }
        return "unsupported_update";
    }

    private Optional<SupportConversationEntity> findConversation(TelegramWebhookUpdate.TelegramWebhookMessage message) {
        if (message.messageThreadId() != null) {
            return conversationRepository.findByTelegramMessageThreadId(message.messageThreadId());
        }
        TelegramWebhookUpdate.TelegramWebhookMessage replyToMessage = message.replyToMessage();
        if (replyToMessage != null && replyToMessage.messageId() != null) {
            return conversationRepository.findByTelegramFallbackMessageId(replyToMessage.messageId());
        }
        return Optional.empty();
    }
}
