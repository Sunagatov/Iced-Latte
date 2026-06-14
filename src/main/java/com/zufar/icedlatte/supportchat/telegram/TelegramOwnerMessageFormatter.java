package com.zufar.icedlatte.supportchat.telegram;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.supportchat.owner.OwnerMessage;

import java.util.UUID;

@Component
class TelegramOwnerMessageFormatter {

    private static final int TELEGRAM_TEXT_LIMIT = 4096;

    String topicName(OwnerMessage message) {
        return "Support " + message.conversationId().toString().substring(0, 8);
    }

    String format(OwnerMessage message) {
        UUID conversationId = message.conversationId();
        String customerName = customerName(message);
        String customerEmail = message.customerEmail();
        UUID messageId = message.messageId();
        String header = """
                Support chat message
                Conversation: %s
                Customer: %s
                Email: %s
                Message ID: %s
                Reply in this topic or reply to this bot message.

                """.formatted(conversationId, customerName, customerEmail, messageId);
        return header + truncateBody(message.body(), TELEGRAM_TEXT_LIMIT - header.length());
    }

    private static String customerName(OwnerMessage message) {
        return message.customerName().isBlank() ? message.customerEmail() : message.customerName();
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
