package com.zufar.icedlatte.supportchat.service;

import static com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus.FAILED;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.turnstile.TurnstileVerificationException;
import com.zufar.icedlatte.common.turnstile.TurnstileVerificationRequest;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.exception.DuplicateSupportChatMessageException;
import com.zufar.icedlatte.supportchat.exception.SupportChatRateLimitExceededException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
class SupportChatCustomerMessagePolicy {

    private final SupportChatProperties properties;
    private final TurnstileVerifier turnstileVerifier;
    private final SupportChatAbuseGuard abuseGuard;
    private final RateLimiter rateLimiter;

    SupportChatCustomerMessagePolicy(
            SupportChatProperties properties,
            TurnstileVerifier turnstileVerifier,
            SupportChatAbuseGuard abuseGuard,
            @Qualifier("openRateLimiter") RateLimiter rateLimiter) {
        this.properties = properties;
        this.turnstileVerifier = turnstileVerifier;
        this.abuseGuard = abuseGuard;
        this.rateLimiter = rateLimiter;
    }

    void enforceCustomerMessageRules(
            UUID userId,
            UUID conversationId,
            String clientIp,
            SupportChatMessageBodyPolicy.MessageContent messageContent,
            SupportMessageEntity previousCustomerMessage,
            String turnstileToken) {
        preventRepeatedMessage(conversationId, messageContent.duplicateKey(), previousCustomerMessage);
        enforceRateLimits(userId, conversationId, clientIp);
        verifyTurnstileIfRequired(conversationId, previousCustomerMessage, turnstileToken, clientIp);
    }

    private void preventRepeatedMessage(
            UUID conversationId, String duplicateKey, SupportMessageEntity previousCustomerMessage) {
        if (previousCustomerMessage == null || previousCustomerMessage.getDeliveryStatus() == FAILED) {
            return;
        }
        if (!duplicateKey.equals(previousCustomerMessage.getNormalizedBody())) {
            return;
        }

        abuseGuard.requireTurnstileForNextMessage(conversationId);
        log.info("support_chat.customer_message.duplicate_rejected: conversationId={}", conversationId);
        throw new DuplicateSupportChatMessageException();
    }

    private void enforceRateLimits(UUID userId, UUID conversationId, String clientIp) {
        SupportChatProperties.RateLimits rateLimits = properties.rateLimits();
        consume(conversationId, "support-chat:user-minute:" + userId, rateLimits.perMinute(), "user-minute");
        consume(conversationId, "support-chat:user-hour:" + userId, rateLimits.perHour(), "user-hour");
        consume(conversationId, "support-chat:user-day:" + userId, rateLimits.perDay(), "user-day");
        consume(
                conversationId,
                "support-chat:conversation-burst:" + conversationId,
                rateLimits.perConversationBurst(),
                "conversation-burst");
        consume(conversationId, "support-chat:ip-minute:" + clientIp, rateLimits.perIp(), "ip-minute");
    }

    private void consume(UUID conversationId, String key, SupportChatProperties.Bucket bucket, String keyType) {
        var result = rateLimiter.tryConsume(key, bucket.maxRequests(), bucket.windowDuration());
        if (result.allowed()) {
            return;
        }
        abuseGuard.requireTurnstileForNextMessage(conversationId);
        log.info("support_chat.customer_message.rate_limited: conversationId={}, keyType={}", conversationId, keyType);
        throw new SupportChatRateLimitExceededException();
    }

    private void verifyTurnstileIfRequired(
            UUID conversationId, SupportMessageEntity previousCustomerMessage, String turnstileToken, String clientIp) {
        if (!abuseGuard.requiresTurnstile(previousCustomerMessage, conversationId)) {
            return;
        }
        try {
            turnstileVerifier.verify(TurnstileVerificationRequest.forAction(turnstileToken, clientIp, "support_chat"));
            abuseGuard.clearTurnstileRequirement(conversationId);
        } catch (TurnstileVerificationException ex) {
            abuseGuard.requireTurnstileForNextMessage(conversationId);
            log.info("support_chat.turnstile.failed: conversationId={}", conversationId);
            throw ex;
        }
    }
}
