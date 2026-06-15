package com.zufar.icedlatte.supportchat.service;

import static com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus.FAILED;
import static com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus.SENT;
import static com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType.CUSTOMER;
import static com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType.OWNER;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.zufar.icedlatte.common.turnstile.TurnstileVerificationException;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.exception.DuplicateSupportChatMessageException;
import com.zufar.icedlatte.supportchat.exception.InvalidSupportChatMessageException;
import com.zufar.icedlatte.supportchat.exception.SupportChatConversationNotFoundException;
import com.zufar.icedlatte.supportchat.exception.SupportChatOwnerDeliveryFailedException;
import com.zufar.icedlatte.supportchat.exception.SupportChatRateLimitExceededException;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageDeliveryResult;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.realtime.SupportChatMessagePublisher;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class SupportChatMessageService {

    private final SupportChatProperties properties;
    private final SupportChatAvailabilityService availabilityService;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final OwnerMessageSender ownerMessageSender;
    private final SupportChatMessagePublisher messagePublisher;
    private final TurnstileVerifier turnstileVerifier;
    private final SupportChatAbuseGuard abuseGuard;
    private final RateLimiter rateLimiter;
    private final PlatformTransactionManager transactionManager;

    public SupportMessageEntity sendCustomerMessage(
            CurrentUserSnapshot user,
            UUID conversationId,
            UUID clientMessageId,
            String body,
            String turnstileToken,
            String clientIp) {
        PendingCustomerMessage pendingMessage = transactionTemplate()
                .execute(_ ->
                        prepareCustomerMessage(user, conversationId, clientMessageId, body, turnstileToken, clientIp));
        Objects.requireNonNull(pendingMessage, "pendingMessage");

        SupportMessageEntity message = pendingMessage.message();
        if (pendingMessage.alreadyAccepted()) {
            return message;
        }

        SupportConversationEntity conversation = pendingMessage.conversation();
        var deliveryResult = sendToOwner(conversation, message, user);
        boolean delivered = deliveryResult.delivered();
        SupportMessageEntity updatedMessage = markCustomerDeliveryStatus(message, delivered);
        String logMessage =
                "support_chat.customer_message.accepted: conversationId={}, messageId={}, ownerDelivered={}";
        log.info(logMessage, conversation.getId(), updatedMessage.getId(), delivered);
        if (!delivered) {
            throw new SupportChatOwnerDeliveryFailedException();
        }
        return updatedMessage;
    }

    @Transactional
    public Optional<SupportMessageEntity> saveOwnerReply(
            SupportConversationEntity conversation, String body, long telegramUpdateId, long telegramMessageId) {
        UUID conversationId = conversation.getId();
        if (messageRepository.existsByTelegramUpdateId(telegramUpdateId)) {
            String logMessage = "support_chat.owner_reply.duplicate_ignored: conversationId={}, telegramUpdateId={}";
            log.info(logMessage, conversationId, telegramUpdateId);
            return Optional.empty();
        }

        String messageBody = normalizeBody(body);
        validateMessage(messageBody);

        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversationId);
        message.setSenderType(OWNER);
        message.setBody(messageBody);
        message.setNormalizedBody(toDuplicateKey(messageBody));
        message.setDeliveryStatus(SENT);
        message.setTelegramUpdateId(telegramUpdateId);
        message.setTelegramMessageId(telegramMessageId);

        SupportMessageEntity saved = messageRepository.save(message);

        conversationRepository.touchLastMessageAt(conversationId);

        messagePublisher.publishOwnerReply(conversation, saved);

        String logMessage = "support_chat.owner_reply.accepted: conversationId={}, messageId={}, telegramUpdateId={}";
        log.info(logMessage, conversationId, saved.getId(), telegramUpdateId);

        return Optional.of(saved);
    }

    private PendingCustomerMessage prepareCustomerMessage(
            CurrentUserSnapshot user,
            UUID conversationId,
            UUID clientMessageId,
            String body,
            String turnstileToken,
            String clientIp) {
        availabilityService.requireAvailable(user);
        UUID userId = user.id();
        SupportConversationEntity conversation = ensureOwnsConversation(userId, conversationId);
        String messageBody = normalizeBody(body);
        validateMessage(messageBody);

        var existing = messageRepository.findByConversationIdAndClientMessageId(conversationId, clientMessageId);
        if (existing.isPresent()) {
            if (existing.get().getDeliveryStatus() == FAILED) {
                throw new SupportChatOwnerDeliveryFailedException();
            }
            return new PendingCustomerMessage(conversation, existing.get(), true);
        }

        SupportMessageEntity previousCustomerMessage =
                findPreviousCustomerMessage(conversationId).orElse(null);
        preventRepeatedMessage(conversationId, messageBody, previousCustomerMessage);
        enforceRateLimits(userId, conversationId, clientIp);
        verifyTurnstileIfRequired(conversationId, previousCustomerMessage, turnstileToken);

        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversation.getId());
        message.setSenderType(CUSTOMER);
        message.setSenderUserId(userId);
        message.setClientMessageId(clientMessageId);
        message.setBody(messageBody);
        message.setNormalizedBody(toDuplicateKey(messageBody));

        SupportMessageEntity saved = messageRepository.save(message);

        conversationRepository.touchLastMessageAt(conversation.getId());

        return new PendingCustomerMessage(conversation, saved, false);
    }

    private SupportMessageEntity markCustomerDeliveryStatus(SupportMessageEntity message, boolean delivered) {
        SupportMessageDeliveryStatus deliveryStatus = delivered ? SENT : FAILED;
        transactionTemplate().executeWithoutResult(_ -> {
            int updatedRows = messageRepository.updateDeliveryStatus(message.getId(), deliveryStatus, !delivered);
            if (updatedRows != 1) {
                throw new IllegalStateException("Support chat message delivery status was not updated");
            }
        });
        message.setDeliveryStatus(deliveryStatus);
        message.setOperatorInspectionRequired(!delivered);
        return message;
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
        if (previousCustomerMessage == null || previousCustomerMessage.getDeliveryStatus() == FAILED) {
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
        if (result.allowed()) {
            return;
        }
        abuseGuard.requireTurnstileForNextMessage(conversationId);
        String logMessage = "support_chat.customer_message.rate_limited: conversationId={}, keyType={}";
        log.info(logMessage, conversationId, keyType);
        throw new SupportChatRateLimitExceededException();
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
        return messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(conversationId, CUSTOMER);
    }

    private OwnerMessageDeliveryResult sendToOwner(
            SupportConversationEntity conversation, SupportMessageEntity saved, CurrentUserSnapshot user) {
        UUID id = conversation.getId();
        UUID messageId = saved.getId();
        try {
            String customerName = user.displayName();
            String email = user.email();
            String body = saved.getBody();
            OwnerMessage ownerMessage = new OwnerMessage(id, messageId, customerName, email, body);

            return ownerMessageSender.send(ownerMessage);
        } catch (RuntimeException ex) {
            String logMessage =
                    "support_chat.owner_message.delivery_failed: conversationId={}, messageId={}, exceptionClass={}";
            String name = ex.getClass().getSimpleName();
            log.warn(logMessage, id, messageId, name);
            return OwnerMessageDeliveryResult.failedResult();
        }
    }

    private static String normalizeBody(String body) {
        return body == null ? "" : body.trim();
    }

    private static String toDuplicateKey(String body) {
        return body.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(false);
        return transactionTemplate;
    }

    private record PendingCustomerMessage(
            SupportConversationEntity conversation, SupportMessageEntity message, boolean alreadyAccepted) {}
}
