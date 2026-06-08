package com.zufar.icedlatte.supportchat.service;

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
import com.zufar.icedlatte.supportchat.exception.SupportChatRateLimitExceededException;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageDeliveryResult;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class SupportChatService {

    private static final int MAX_PAGE_SIZE = 50;

    private final SupportChatProperties properties;
    private final SupportChatEligibilityService eligibilityService;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final OwnerMessageSender ownerMessageSender;
    private final TurnstileVerifier turnstileVerifier;

    private final RateLimiter rateLimiter;

    public SupportChatService(
            SupportChatProperties properties,
            SupportChatEligibilityService eligibilityService,
            SupportConversationRepository conversationRepository,
            SupportMessageRepository messageRepository,
            OwnerMessageSender ownerMessageSender,
            TurnstileVerifier turnstileVerifier,
            @Qualifier("openRateLimiter") RateLimiter rateLimiter) {
        this.properties = properties;
        this.eligibilityService = eligibilityService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.ownerMessageSender = ownerMessageSender;
        this.turnstileVerifier = turnstileVerifier;
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

    @Transactional
    public SupportMessageEntity sendCustomerMessage(
            CurrentUserSnapshot user, UUID conversationId, UUID clientMessageId, String body, String turnstileToken) {
        ensureAvailable(user);
        SupportConversationEntity conversation = ensureOwnsConversation(user.id(), conversationId);
        String messageBody = normalizeBody(body);
        validateMessage(messageBody);

        var existing = messageRepository.findByConversationIdAndClientMessageId(conversationId, clientMessageId);
        if (existing.isPresent()) {
            return existing.get();
        }

        preventRepeatedMessage(conversationId, messageBody);
        enforceRateLimits(user.id(), conversationId);
        verifyTurnstileIfRequired(conversationId, turnstileToken);

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
        saved.setDeliveryStatus(
                deliveryResult.delivered() ? SupportMessageDeliveryStatus.SENT : SupportMessageDeliveryStatus.FAILED);
        log.info(
                "support_chat.customer_message.accepted: conversationId={}, messageId={}, ownerDelivered={}",
                conversation.getId(),
                saved.getId(),
                deliveryResult.delivered());
        return saved;
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

    private void preventRepeatedMessage(UUID conversationId, String normalizedBody) {
        String duplicateCandidate = toDuplicateKey(normalizedBody);
        messageRepository
                .findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        conversationId, SupportMessageSenderType.CUSTOMER)
                .filter(previous -> previous.getNormalizedBody().equals(duplicateCandidate))
                .ifPresent(_ -> {
                    throw new DuplicateSupportChatMessageException();
                });
    }

    private void enforceRateLimits(UUID userId, UUID conversationId) {
        SupportChatProperties.RateLimits rateLimits = properties.rateLimits();
        consume("support-chat:user-minute:" + userId, rateLimits.perMinute());
        consume("support-chat:user-hour:" + userId, rateLimits.perHour());
        consume("support-chat:user-day:" + userId, rateLimits.perDay());
        consume("support-chat:conversation-burst:" + conversationId, rateLimits.perConversationBurst());
    }

    private void consume(String key, SupportChatProperties.Bucket bucket) {
        var result = rateLimiter.tryConsume(key, bucket.maxRequests(), bucket.windowDuration());
        if (!result.allowed()) {
            throw new SupportChatRateLimitExceededException();
        }
    }

    private void verifyTurnstileIfRequired(UUID conversationId, String turnstileToken) {
        if (!properties.turnstile().firstMessageEnabled()) {
            return;
        }
        boolean hasMessages = messageRepository
                .findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        conversationId, SupportMessageSenderType.CUSTOMER)
                .isPresent();
        if (!hasMessages) {
            turnstileVerifier.verify(turnstileToken);
        }
    }

    private OwnerMessageDeliveryResult sendToOwner(
            SupportConversationEntity conversation, SupportMessageEntity saved, CurrentUserSnapshot user) {
        try {
            return ownerMessageSender.send(
                    new OwnerMessage(conversation.getId(), saved.getId(), user.id(), user.email(), saved.getBody()));
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
