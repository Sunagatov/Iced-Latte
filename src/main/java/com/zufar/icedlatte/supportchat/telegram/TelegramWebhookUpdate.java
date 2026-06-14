package com.zufar.icedlatte.supportchat.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record TelegramWebhookUpdate(@JsonProperty("update_id") Long updateId, TelegramWebhookMessage message) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TelegramWebhookMessage(
            @JsonProperty("message_id") Long messageId,
            @JsonProperty("message_thread_id") Long messageThreadId,
            TelegramWebhookChat chat,
            TelegramWebhookUser from,
            String text,
            @JsonProperty("reply_to_message") TelegramWebhookMessage replyToMessage,
            @JsonProperty("forward_origin") Object forwardOrigin,
            @JsonProperty("forward_from") TelegramWebhookUser forwardFrom,
            @JsonProperty("forward_sender_name") String forwardSenderName,
            @JsonProperty("forward_date") Long forwardDate) {

        TelegramWebhookMessage(
                Long messageId,
                Long messageThreadId,
                TelegramWebhookChat chat,
                TelegramWebhookUser from,
                String text,
                TelegramWebhookMessage replyToMessage) {
            this(messageId, messageThreadId, chat, from, text, replyToMessage, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TelegramWebhookChat(Long id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TelegramWebhookUser(
            Long id, @JsonProperty("is_bot") Boolean bot) {}
}
