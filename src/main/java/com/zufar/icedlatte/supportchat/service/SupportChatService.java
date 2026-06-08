package com.zufar.icedlatte.supportchat.service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.turnstile.TurnstileVerificationException;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType;
import com.zufar.icedlatte.supportchat.exception.DuplicateSupportChatMessageException;
import com.zufar.icedlatte.supportchat.exception.InvalidSupportChatMessageException;
import com.zufar.icedlatte.supportchat.exception.SupportChatConversationNotFoundException;
import com.zufar.icedlatte.supportchat.exception.SupportChatDisabledException;
import com.zufar.icedlatte.supportchat.exception.SupportChatEmailVerificationRequiredException;
import com.zufar.icedlatte.supportchat.exception.SupportChatOwnerDeliveryFailedException;
import com.zufar.icedlatte.supportchat.exception.SupportChatRateLimitExceededException;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageDeliveryResult;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.realtime.SupportChatMessagePublisher;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SupportChatService {

    private static final int MAX_PAGE_SIZE = 50;

    private final SupportChatProperties properties;
    private final SupportChatEligibilityService eligibilityService;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final OwnerMessageSender ownerMessageSender;
    private final SupportChatMessagePublisher messagePublisher;
    private final TurnstileVerifier turnstileVerifier;
    private final SupportChatAbuseGuard abuseGuard;

    private final RateLimiter rateLimiter;

    public SupportChatService(
            SupportChatProperties properties,
            SupportChatEligibilityService eligibilityService,
            SupportConversationRepository conversationRepository,
            SupportMessageRepository messageRepository,
            OwnerMessageSender ownerMessageSender,
            SupportChatMessagePublisher messagePublisher,
            TurnstileVerifier turnstileVerifier,
            SupportChatAbuseGuard abuseGuard,
            @Qualifier("openRateLimiter") RateLimiter rateLimiter) {
        this.properties = properties;
        this.eligibilityService = eligibilityService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ownerMessageSender = ownerMessageSender;
        this.messagePublisher = messagePublisher;
        this.turnstileVerifier = turnstileVerifier;
        this.abuseGuard = abuseGuard;
        this.rateLimiter = rateLimiter;
    }

    @Transactional(readOnly = true)
    public SupportChatStatus status(CurrentUserSnapshot user) {
        if (!properties.enabled()) {
            return new SupportChatStatus(false, false, null);
        }
        SupportChatEligibility eligibility = eligibilityService.eligibilityFor(user.id());
        return new SupportChatStatus(true, eligibility.eligible(), eligibility.reason());
    }

    @Transactional
    public SupportConversationEntity getOrCreateConversation(CurrentUserSnapshot user) {
        ensureAvailable(user);
        return conversationRepository.findByUserId(user.id()).orElseGet(() -> {
            conversationRepository.insertOpenConversationIfAbsent(UUID.randomUUID(), user.id());
            conversationRepository.flush();
            return conversationRepository
                    .findByUserId(user.id())
                    .orElseThrow(() -> new IllegalStateException("Support conversation was not created"));
        });
    }

    @Transactional(readOnly = true)
    public Page<SupportMessageEntity> getHistory(CurrentUserSnapshot user, UUID conversationId, int page, int size) {
        ensureAvailable(user);
        ensureOwnsConversation(user.id(), conversationId);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        OffsetDateTime createdAfter = OffsetDateTime.now().minusDays(properties.retentionDays());
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        return messageRepository.findByConversationIdAndVisibleToCustomerTrueAndCreatedAtAfter(
                conversationId, createdAfter, pageable);
    }

    @Transactional(noRollbackFor = SupportChatOwnerDeliveryFailedException.class)
    public SupportMessageEntity sendCustomerMessage(
            CurrentUserSnapshot user,
            UUID conversationId,
            UUID clientMessageId,
            String body,
            String turnstileToken,
            String clientIp) {
        ensureAvailable(user);
        SupportConversationEntity conversation = ensureOwnsConversation(user.id(), conversationId);
        String messageBody = normalizeBody(body);
        validateMessage(messageBody);

        var existing = messageRepository.findByConversationIdAndClientMessageId(conversationId, clientMessageId);
        if (existing.isPresent()) {
            if (existing.get().getDeliveryStatus() == SupportMessageDeliveryStatus.FAILED) {
                throw new SupportChatOwnerDeliveryFailedException();
            }
            return existing.get();
        }

        SupportMessageEntity previousCustomerMessage =
                findPreviousCustomerMessage(conversationId).orElse(null);
        preventRepeatedMessage(conversationId, messageBody, previousCustomerMessage);
        enforceRateLimits(user.id(), conversationId, clientIp);
        verifyTurnstileIfRequired(conversationId, previousCustomerMessage, turnstileToken);

        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversation.getId());
        message.setSenderType(SupportMessageSenderType.CUSTOMER);
        message.setSenderUserId(user.id());
        message.setClientMessageId(clientMessageId);
        message.setBody(messageBody);
        message.setNormalizedBody(toDuplicateKey(messageBody));

        SupportMessageEntity saved = messageRepository.save(message);

        conversationRepository.touchLastMessageAt(conversation.getId());

        var deliveryResult = sendToOwner(conversation, saved, user);
        SupportMessageDeliveryStatus deliveryStatus =
                deliveryResult.delivered() ? SupportMessageDeliveryStatus.SENT : SupportMessageDeliveryStatus.FAILED;
        saved.setDeliveryStatus(deliveryStatus);
        log.info(
                "support_chat.customer_message.accepted: conversationId={}, messageId={}, ownerDelivered={}",
                conversation.getId(),
                saved.getId(),
                deliveryResult.delivered());
        if (!deliveryResult.delivered()) {
            throw new SupportChatOwnerDeliveryFailedException();
        }
        return saved;
    }

    @Transactional
    public Optional<SupportMessageEntity> saveOwnerReply(
            SupportConversationEntity conversation, String body, long telegramUpdateId, long telegramMessageId) {
        if (messageRepository.existsByTelegramUpdateId(telegramUpdateId)) {
            log.info(
                    "support_chat.owner_reply.duplicate_ignored: conversationId={}, telegramUpdateId={}",
                    conversation.getId(),
                    telegramUpdateId);
            return Optional.empty();
        }

        String messageBody = normalizeBody(body);
        validateMessage(messageBody);

        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversation.getId());
        message.setSenderType(SupportMessageSenderType.OWNER);
        message.setBody(messageBody);
        message.setNormalizedBody(toDuplicateKey(messageBody));
        message.setDeliveryStatus(SupportMessageDeliveryStatus.SENT);
        message.setTelegramUpdateId(telegramUpdateId);
        message.setTelegramMessageId(telegramMessageId);
        SupportMessageEntity saved = messageRepository.save(message);

        conversationRepository.touchLastMessageAt(conversation.getId());

        messagePublisher.publishOwnerReply(conversation, saved);

        log.info(
                "support_chat.owner_reply.accepted: conversationId={}, messageId={}, telegramUpdateId={}",
                conversation.getId(),
                saved.getId(),
                telegramUpdateId);

        return Optional.of(saved);
    }

    private void ensureAvailable(CurrentUserSnapshot user) {
        if (!properties.enabled()) {
            throw new SupportChatDisabledException();
        }
        SupportChatEligibility eligibility = eligibilityService.eligibilityFor(user.id());
        if (!eligibility.eligible()) {
            throw new SupportChatEmailVerificationRequiredException();
        }
    }

    private SupportConversationEntity ensureOwnsConversation(UUID userId, UUID conversationId) {
        return conversationRepository
                .findById(conversationId)
                .filter(conversation -> conversation.getUserId().equals(userId))
                .orElseThrow(SupportChatConversationNotFoundException::new);
    }

    private void validateMessage(String normalizedBody) {
        if (normalizedBody.isBlank()) {
            throw new InvalidSupportChatMessageException("Message body must not be blank.");
        }
        if (normalizedBody.length() > properties.messageMaxLength()) {
            throw new InvalidSupportChatMessageException("Message body is too long.");
        }
    }

    private void preventRepeatedMessage(
            UUID conversationId, String normalizedBody, SupportMessageEntity previousCustomerMessage) {
        if (previousCustomerMessage == null
                || previousCustomerMessage.getDeliveryStatus() == SupportMessageDeliveryStatus.FAILED) {
            return;
        }

        String duplicateCandidate = toDuplicateKey(normalizedBody);
        if (!previousCustomerMessage.getNormalizedBody().equals(duplicateCandidate)) {
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
        if (!result.allowed()) {
            abuseGuard.requireTurnstileForNextMessage(conversationId);
            log.info(
                    "support_chat.customer_message.rate_limited: conversationId={}, keyType={}",
                    conversationId,
                    keyType);
            throw new SupportChatRateLimitExceededException();
        }
    }

    private void verifyTurnstileIfRequired(
            UUID conversationId, SupportMessageEntity previousCustomerMessage, String turnstileToken) {
        if (!abuseGuard.requiresTurnstile(previousCustomerMessage, conversationId)) {
            return;
        }
        try {
            turnstileVerifier.verify(turnstileToken);
            abuseGuard.clearTurnstileRequirement(conversationId);
        } catch (TurnstileVerificationException ex) {
            abuseGuard.requireTurnstileForNextMessage(conversationId);
            log.info("support_chat.turnstile.failed: conversationId={}", conversationId);
            throw ex;
        }
    }

    private Optional<SupportMessageEntity> findPreviousCustomerMessage(UUID conversationId) {
        return messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                conversationId, SupportMessageSenderType.CUSTOMER);
    }

    private OwnerMessageDeliveryResult sendToOwner(
            SupportConversationEntity conversation, SupportMessageEntity saved, CurrentUserSnapshot user) {
        try {
            OwnerMessage ownerMessage = new OwnerMessage(
                    conversation.getId(), saved.getId(), user.displayName(), user.email(), saved.getBody());

            return ownerMessageSender.send(ownerMessage);
        } catch (RuntimeException ex) {
            log.warn(
                    "support_chat.owner_message.delivery_failed: conversationId={}, messageId={}, exceptionClass={}",
                    conversation.getId(),
                    saved.getId(),
                    ex.getClass().getSimpleName());
            return OwnerMessageDeliveryResult.failedResult();
        }
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

    public record SupportChatStatus(boolean enabled, boolean eligible, String reason) {}
}
