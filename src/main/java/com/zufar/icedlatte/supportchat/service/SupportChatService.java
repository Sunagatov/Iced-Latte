package com.zufar.icedlatte.supportchat.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.exception.SupportChatConversationNotFoundException;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportChatService {

    private static final int MAX_PAGE_SIZE = 50;

    private final SupportChatProperties properties;
    private final SupportChatAvailabilityService availabilityService;
    private final SupportChatEligibilityService eligibilityService;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportChatMessageService messageService;

    public SupportChatStatus status(CurrentUserSnapshot user) {
        if (!properties.enabled()) {
            return new SupportChatStatus(false, false, null);
        }
        SupportChatEligibility eligibility = eligibilityService.eligibilityFor(user.id());
        return new SupportChatStatus(true, eligibility.eligible(), eligibility.reason());
    }

    @Transactional
    public SupportConversationEntity getOrCreateConversation(CurrentUserSnapshot user) {
        availabilityService.requireAvailable(user);
        UUID userId = user.id();
        return conversationRepository.findByUserId(userId).orElseGet(() -> {
            conversationRepository.insertOpenConversationIfAbsent(UUID.randomUUID(), userId);
            conversationRepository.flush();
            return conversationRepository
                    .findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Support conversation was not created"));
        });
    }

    @Transactional(readOnly = true)
    public Page<SupportMessageEntity> getHistory(CurrentUserSnapshot user, UUID conversationId, int page, int size) {
        availabilityService.requireAvailable(user);
        ensureOwnsConversation(user.id(), conversationId);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        OffsetDateTime createdAfter = OffsetDateTime.now().minusDays(properties.retentionDays());
        Sort sortObject = Sort.by(Sort.Direction.ASC, "createdAt");
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, sortObject);
        return messageRepository.findByConversationIdAndVisibleToCustomerTrueAndCreatedAtAfter(
                conversationId, createdAfter, pageable);
    }

    public SupportMessageEntity sendCustomerMessage(
            CurrentUserSnapshot user,
            UUID conversationId,
            UUID clientMessageId,
            String body,
            String turnstileToken,
            String clientIp) {
        return messageService.sendCustomerMessage(
                user, conversationId, clientMessageId, body, turnstileToken, clientIp);
    }

    @Transactional
    public Optional<SupportMessageEntity> saveOwnerReply(
            SupportConversationEntity conversation, String body, long telegramUpdateId, long telegramMessageId) {
        return messageService.saveOwnerReply(conversation, body, telegramUpdateId, telegramMessageId);
    }

    private void ensureOwnsConversation(UUID userId, UUID conversationId) {
        if (!conversationRepository.existsByIdAndUserId(conversationId, userId)) {
            throw new SupportChatConversationNotFoundException();
        }
    }

    public record SupportChatStatus(boolean enabled, boolean eligible, String reason) {}
}
