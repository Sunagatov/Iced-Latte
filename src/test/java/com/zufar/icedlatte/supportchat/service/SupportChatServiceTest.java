package com.zufar.icedlatte.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.ratelimit.api.RateLimitResult;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Bucket;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.RateLimits;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("SupportChatService unit tests")
class SupportChatServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID CLIENT_MESSAGE_ID = UUID.randomUUID();
    private static final CurrentUserSnapshot USER = new CurrentUserSnapshot(USER_ID, "customer@example.com");

    @Mock
    private SupportChatEligibilityService eligibilityService;

    @Mock
    private SupportConversationRepository conversationRepository;

    @Mock
    private SupportMessageRepository messageRepository;

    @Mock
    private OwnerMessageSender ownerMessageSender;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private RateLimiter rateLimiter;

    @Test
    @DisplayName("Status reports disabled feature as unavailable")
    void status_disabled_reportsUnavailable() {
        var result = disabledService().status(USER);

        assertThat(result.enabled()).isFalse();
        assertThat(result.eligible()).isFalse();
        assertThat(result.reason()).isNull();
        verifyNoInteractions(eligibilityService);
    }

    @Test
    @DisplayName("Get or create reuses existing permanent conversation")
    void getOrCreateConversation_existing_returnsExisting() {
        SupportConversationEntity conversation = conversation();
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(conversation));

        var result = enabledService().getOrCreateConversation(USER);

        assertThat(result).isSameAs(conversation);
        verify(conversationRepository, never()).insertOpenConversationIfAbsent(any(), any());
    }

    @Test
    @DisplayName("Get or create inserts one permanent conversation when absent")
    void getOrCreateConversation_absent_insertsPermanentConversation() {
        SupportConversationEntity conversation = conversation();
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findByUserId(USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(conversation));

        var result = enabledService().getOrCreateConversation(USER);

        assertThat(result).isSameAs(conversation);
        verify(conversationRepository).insertOpenConversationIfAbsent(any(UUID.class), eq(USER_ID));
        verify(conversationRepository).flush();
    }

    @Test
    @DisplayName("Unverified or ineligible user cannot create a conversation")
    void getOrCreateConversation_ineligible_throwsForbidden() {
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.emailVerificationRequired());

        assertThatThrownBy(() -> enabledService().getOrCreateConversation(USER))
                .isInstanceOf(SupportChatEmailVerificationRequiredException.class);
    }

    @Test
    @DisplayName("Sending message persists before owner delivery and marks delivered")
    void sendCustomerMessage_valid_persistsAndSendsToOwner() {
        SupportConversationEntity conversation = conversation();
        SupportMessageEntity saved = savedCustomerMessage("Hello support");
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(ownerMessageSender.send(any(OwnerMessage.class))).thenReturn(OwnerMessageDeliveryResult.deliveredResult());

        var result = enabledService()
                .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "  Hello   support  ", null);

        assertThat(result.getDeliveryStatus()).isEqualTo(SupportMessageDeliveryStatus.SENT);
        ArgumentCaptor<SupportMessageEntity> messageCaptor = ArgumentCaptor.forClass(SupportMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getBody()).isEqualTo("Hello   support");
        assertThat(messageCaptor.getValue().getNormalizedBody()).isEqualTo("hello support");
        verify(conversationRepository).touchLastMessageAt(CONVERSATION_ID);
        verify(ownerMessageSender).send(any(OwnerMessage.class));
    }

    @Test
    @DisplayName("Owner delivery exception does not roll back accepted customer message")
    void sendCustomerMessage_ownerSenderThrows_marksFailedWithoutLeakingException() {
        SupportMessageEntity saved = savedCustomerMessage("Hello");
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(ownerMessageSender.send(any(OwnerMessage.class)))
                .thenThrow(new IllegalStateException("owner unavailable"));

        var result = enabledService().sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null);

        assertThat(result.getDeliveryStatus()).isEqualTo(SupportMessageDeliveryStatus.FAILED);
        verify(conversationRepository).touchLastMessageAt(CONVERSATION_ID);
    }

    @Test
    @DisplayName("Owner reply is persisted as visible sent message with Telegram correlation")
    void saveOwnerReply_valid_persistsVisibleOwnerMessage() {
        SupportConversationEntity conversation = conversation();
        SupportMessageEntity saved = savedOwnerMessage("Owner answer");
        when(messageRepository.existsByTelegramUpdateId(9001L)).thenReturn(false);
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);

        var result = enabledService().saveOwnerReply(conversation, "  Owner answer  ", 9001L, 7001L);

        assertThat(result).contains(saved);
        ArgumentCaptor<SupportMessageEntity> messageCaptor = ArgumentCaptor.forClass(SupportMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getConversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(messageCaptor.getValue().getSenderType()).isEqualTo(SupportMessageSenderType.OWNER);
        assertThat(messageCaptor.getValue().getBody()).isEqualTo("Owner answer");
        assertThat(messageCaptor.getValue().getNormalizedBody()).isEqualTo("owner answer");
        assertThat(messageCaptor.getValue().getDeliveryStatus()).isEqualTo(SupportMessageDeliveryStatus.SENT);
        assertThat(messageCaptor.getValue().getTelegramUpdateId()).isEqualTo(9001L);
        assertThat(messageCaptor.getValue().getTelegramMessageId()).isEqualTo(7001L);
        verify(conversationRepository).touchLastMessageAt(CONVERSATION_ID);
    }

    @Test
    @DisplayName("Duplicate Telegram owner reply is ignored without persistence")
    void saveOwnerReply_duplicateTelegramUpdate_ignoresMessage() {
        when(messageRepository.existsByTelegramUpdateId(9001L)).thenReturn(true);

        var result = enabledService().saveOwnerReply(conversation(), "Owner answer", 9001L, 7001L);

        assertThat(result).isEmpty();
        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).touchLastMessageAt(any());
    }

    @Test
    @DisplayName("Disabled support chat rejects message before persistence")
    void sendCustomerMessage_disabled_throwsNotFound() {
        assertThatThrownBy(() ->
                        disabledService().sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null))
                .isInstanceOf(SupportChatDisabledException.class);

        verify(messageRepository, never()).save(any());
        verify(ownerMessageSender, never()).send(any());
    }

    @Test
    @DisplayName("Blank message is rejected before rate limiting and persistence")
    void sendCustomerMessage_blankBody_throwsBadRequest() {
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() ->
                        enabledService().sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "   ", null))
                .isInstanceOf(InvalidSupportChatMessageException.class);

        verify(rateLimiter, never()).tryConsume(any(), anyInt(), any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Messages for another user's conversation are rejected")
    void sendCustomerMessage_foreignConversation_throwsNotFound() {
        SupportConversationEntity conversation = conversation();
        conversation.setUserId(UUID.randomUUID());
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() ->
                        enabledService().sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null))
                .isInstanceOf(SupportChatConversationNotFoundException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sending with same client message ID is idempotent")
    void sendCustomerMessage_existingClientMessageId_returnsExistingMessage() {
        SupportMessageEntity existing = savedCustomerMessage("Already accepted");
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.of(existing));

        var result = enabledService()
                .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Already accepted", null);

        assertThat(result).isSameAs(existing);
        verify(messageRepository, never()).save(any());
        verify(ownerMessageSender, never()).send(any());
    }

    @Test
    @DisplayName("Repeated identical customer message is rejected")
    void sendCustomerMessage_repeatedIdenticalBody_throwsConflict() {
        SupportMessageEntity previous = savedCustomerMessage("Hello");
        previous.setNormalizedBody("hello");
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.of(previous));

        assertThatThrownBy(() ->
                        enabledService().sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, " hello ", null))
                .isInstanceOf(DuplicateSupportChatMessageException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rate-limited message is rejected before persistence")
    void sendCustomerMessage_rateLimited_throwsTooManyRequests() {
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(blockedRateLimit());

        assertThatThrownBy(() ->
                        enabledService().sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null))
                .isInstanceOf(SupportChatRateLimitExceededException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("First customer message verifies Turnstile when enabled")
    void sendCustomerMessage_firstMessageWithTurnstileEnabled_verifiesToken() {
        SupportMessageEntity saved = savedCustomerMessage("Hello");
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(ownerMessageSender.send(any(OwnerMessage.class))).thenReturn(OwnerMessageDeliveryResult.deliveredResult());

        turnstileEnabledService()
                .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", "turnstile-token");

        verify(turnstileVerifier).verify("turnstile-token");
    }

    @Test
    @DisplayName("History uses bounded page size and retention window")
    void getHistory_usesBoundedPageSizeAndRetentionWindow() {
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndVisibleToCustomerTrueAndCreatedAtAfter(
                        eq(CONVERSATION_ID), any(OffsetDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of()));

        enabledService().getHistory(USER, CONVERSATION_ID, -1, 500);

        verify(messageRepository)
                .findByConversationIdAndVisibleToCustomerTrueAndCreatedAtAfter(
                        eq(CONVERSATION_ID),
                        any(OffsetDateTime.class),
                        argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 50));
    }

    private SupportChatService enabledService() {
        return new SupportChatService(
                properties(true),
                eligibilityService,
                conversationRepository,
                messageRepository,
                ownerMessageSender,
                turnstileVerifier,
                rateLimiter);
    }

    private SupportChatService disabledService() {
        return new SupportChatService(
                properties(false),
                eligibilityService,
                conversationRepository,
                messageRepository,
                ownerMessageSender,
                turnstileVerifier,
                rateLimiter);
    }

    private SupportChatService turnstileEnabledService() {
        return new SupportChatService(
                properties(true, true),
                eligibilityService,
                conversationRepository,
                messageRepository,
                ownerMessageSender,
                turnstileVerifier,
                rateLimiter);
    }

    private static SupportChatProperties properties(boolean enabled) {
        return properties(enabled, false);
    }

    private static SupportChatProperties properties(boolean enabled, boolean turnstileEnabled) {
        return new SupportChatProperties(
                enabled,
                4000,
                90,
                OwnerMessageMode.FAKE,
                new Telegram("", "", 0L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5)),
                new Turnstile(turnstileEnabled),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10))));
    }

    private static SupportConversationEntity conversation() {
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(CONVERSATION_ID);
        conversation.setUserId(USER_ID);
        conversation.setCreatedAt(OffsetDateTime.now());
        conversation.setUpdatedAt(OffsetDateTime.now());
        return conversation;
    }

    private static SupportMessageEntity savedCustomerMessage(String body) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversationId(CONVERSATION_ID);
        message.setClientMessageId(CLIENT_MESSAGE_ID);
        message.setSenderType(SupportMessageSenderType.CUSTOMER);
        message.setSenderUserId(USER_ID);
        message.setBody(body);
        message.setNormalizedBody(body.toLowerCase());
        message.setDeliveryStatus(SupportMessageDeliveryStatus.PENDING);
        message.setVisibleToCustomer(true);
        message.setCreatedAt(OffsetDateTime.now());
        return message;
    }

    private static SupportMessageEntity savedOwnerMessage(String body) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversationId(CONVERSATION_ID);
        message.setSenderType(SupportMessageSenderType.OWNER);
        message.setBody(body);
        message.setNormalizedBody(body.toLowerCase());
        message.setDeliveryStatus(SupportMessageDeliveryStatus.SENT);
        message.setVisibleToCustomer(true);
        message.setCreatedAt(OffsetDateTime.now());
        return message;
    }

    private static RateLimitResult allowedRateLimit() {
        return new RateLimitResult(true, 20, 19, System.currentTimeMillis() + 60_000, 60);
    }

    private static RateLimitResult blockedRateLimit() {
        return new RateLimitResult(false, 20, 0, System.currentTimeMillis() + 60_000, 60);
    }
}
