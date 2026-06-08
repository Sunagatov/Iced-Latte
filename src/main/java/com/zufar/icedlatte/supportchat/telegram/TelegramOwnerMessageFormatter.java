package com.zufar.icedlatte.supportchat.telegram;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.supportchat.owner.OwnerMessage;

@Component
class TelegramOwnerMessageFormatter {

    private static final int TELEGRAM_TEXT_LIMIT = 4096;

    String topicName(OwnerMessage message) {
        return "Support " + message.conversationId().toString().substring(0, 8);
    }

    String format(OwnerMessage message) {
        String header =
                """
                Support chat message
                Conversation: %s
                Customer: %s
                User ID: %s
                Message ID: %s

                """.formatted(message.conversationId(), message.customerEmail(), message.userId(), message.messageId());
        return header + truncateBody(message.body(), TELEGRAM_TEXT_LIMIT - header.length());
    }

    private static String truncateBody(String body, int maxLength) {
        if (body.length() <= maxLength) {
            return body;
        }
        if (maxLength <= 3) {
            return body.substring(0, Math.max(maxLength, 0));
        }
        return body.substring(0, maxLength - 3) + "...";
    }
}
