package com.zufar.icedlatte.supportchat.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.exception.SupportChatException;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;
import com.zufar.icedlatte.supportchat.service.SupportChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class TelegramWebhookService {

    private final SupportChatProperties properties;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
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

        TelegramWebhookUpdate safeUpdate = Objects.requireNonNull(update, "update");
        TelegramWebhookUpdate.TelegramWebhookMessage safeMessage = Objects.requireNonNull(message, "message");
        Optional<SupportConversationEntity> conversation = findConversation(safeMessage);
        if (conversation.isEmpty()) {
            log.warn(
                    "support_chat.telegram.webhook.conversation_not_found: telegramUpdateId={}", safeUpdate.updateId());
            return TelegramWebhookResult.IGNORED;
        }

        try {
            supportChatService.saveOwnerReply(
                    conversation.get(), safeMessage.text(), safeUpdate.updateId(), safeMessage.messageId());
            return TelegramWebhookResult.PROCESSED;
        } catch (SupportChatException | DataIntegrityViolationException ex) {
            String logMessage = "support_chat.telegram.webhook.rejected: telegramUpdateId={}, exceptionClass={}";
            log.warn(logMessage, safeUpdate.updateId(), ex.getClass().getSimpleName());
            return TelegramWebhookResult.IGNORED;
        }
    }

    private boolean isAuthorizedSecret(String secretToken) {
        String expected = properties.telegram().webhookSecret();
        if (expected == null || expected.isBlank() || secretToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), secretToken.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isSupportedOwnerReply(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        String text = message == null ? null : message.text();
        TelegramWebhookUpdate.TelegramWebhookChat chat = message == null ? null : message.chat();
        TelegramWebhookUpdate.TelegramWebhookUser from = message == null ? null : message.from();
        if (isMissingRequiredMetadata(update, message)
                || text == null
                || text.isBlank()
                || hasForwardMetadata(message)
                || isCommand(text)
                || chat == null
                || !Objects.equals(properties.telegram().chatId(), String.valueOf(chat.id()))) {
            return false;
        }
        SupportChatProperties.Telegram chatProperties = properties.telegram();
        return from != null
                && Objects.equals(chatProperties.ownerUserId(), from.id())
                && !Boolean.TRUE.equals(from.bot());
    }

    private void logUnsupportedOwnerReply(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        String logMessage =
                "support_chat.telegram.webhook.unsupported_update: telegramUpdateId={}, telegramMessageId={}, reason={}";
        String replyReason = unsupportedOwnerReplyReason(update, message);
        log.warn(
                logMessage,
                update == null ? null : update.updateId(),
                message == null ? null : message.messageId(),
                replyReason);
    }

    private String unsupportedOwnerReplyReason(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        if (update == null || update.updateId() == null) {
            return "missing_update_id";
        }
        if (message == null || message.messageId() == null) {
            return "missing_message";
        }
        String string = message.text();
        if (string == null || string.isBlank()) {
            return "non_text_message";
        }
        if (hasForwardMetadata(message)) {
            return "forwarded_message";
        }
        if (isCommand(string)) {
            return "command_message";
        }
        TelegramWebhookUpdate.TelegramWebhookChat chat = message.chat();
        if (chat == null) {
            return "missing_chat";
        }
        SupportChatProperties.Telegram chatProperties = properties.telegram();
        if (!Objects.equals(chatProperties.chatId(), String.valueOf(chat.id()))) {
            return "unexpected_chat";
        }
        TelegramWebhookUpdate.TelegramWebhookUser from = message.from();
        if (from == null) {
            return "missing_sender";
        }
        if (!Objects.equals(chatProperties.ownerUserId(), from.id())) {
            return "unexpected_sender";
        }
        if (Boolean.TRUE.equals(from.bot())) {
            return "bot_sender";
        }
        return "unsupported_update";
    }

    private static boolean isMissingRequiredMetadata(
            TelegramWebhookUpdate update, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        return update == null || update.updateId() == null || message == null || message.messageId() == null;
    }

    private Optional<SupportConversationEntity> findConversation(TelegramWebhookUpdate.TelegramWebhookMessage message) {
        Long messageThreadId = message.messageThreadId();
        if (messageThreadId != null) {
            Optional<SupportConversationEntity> conversation =
                    conversationRepository.findByTelegramMessageThreadId(messageThreadId);
            if (conversation.isPresent()) {
                return conversation;
            }
        }
        TelegramWebhookUpdate.TelegramWebhookMessage replyToMessage = message.replyToMessage();
        if (replyToMessage == null || replyToMessage.messageId() == null) {
            return Optional.empty();
        }
        Optional<SupportConversationEntity> conversation = messageRepository
                .findByTelegramMessageId(replyToMessage.messageId())
                .flatMap(correlation -> conversationRepository.findById(correlation.getConversationId()));
        if (conversation.isPresent()) {
            return conversation;
        }
        return conversationRepository.findByTelegramFallbackMessageId(replyToMessage.messageId());
    }

    private static boolean hasForwardMetadata(TelegramWebhookUpdate.TelegramWebhookMessage message) {
        return message.forwardOrigin() != null
                || message.forwardFrom() != null
                || message.forwardSenderName() != null
                || message.forwardDate() != null;
    }

    private static boolean isCommand(String text) {
        return text.stripLeading().startsWith("/");
    }
}
