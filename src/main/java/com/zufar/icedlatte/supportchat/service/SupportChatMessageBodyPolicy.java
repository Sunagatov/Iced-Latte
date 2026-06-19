package com.zufar.icedlatte.supportchat.service;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.exception.InvalidSupportChatMessageException;

@Component
class SupportChatMessageBodyPolicy {

    private final SupportChatProperties properties;

    SupportChatMessageBodyPolicy(SupportChatProperties properties) {
        this.properties = properties;
    }

    MessageContent normalizeAndValidate(String body) {
        String normalizedBody = normalizeBody(body);
        if (normalizedBody.isBlank()) {
            throw new InvalidSupportChatMessageException("Message body must not be blank.");
        }
        if (normalizedBody.length() > properties.messageMaxLength()) {
            throw new InvalidSupportChatMessageException("Message body is too long.");
        }
        return new MessageContent(normalizedBody, toDuplicateKey(normalizedBody));
    }

    private static String normalizeBody(String body) {
        if (body == null) {
            return "";
        }
        return body.trim();
    }

    private static String toDuplicateKey(String body) {
        return body.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    record MessageContent(String body, String duplicateKey) {}
}
