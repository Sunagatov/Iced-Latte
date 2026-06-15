package com.zufar.icedlatte.supportchat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
import com.zufar.icedlatte.supportchat.exception.SupportChatAccessRestrictedException;
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
    private SupportChatAvailabilityService availabilityService;

    @Mock
    private SupportConversationRepository conversationRepository;

    @Mock
    private SupportMessageRepository messageRepository;

    @Mock
    private OwnerMessageSender ownerMessageSender;

    @Mock
    private SupportChatMessagePublisher messagePublisher;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private PlatformTransactionManager transactionManager;

    @org.junit.jupiter.api.BeforeEach
    void setUpTransactions() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

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
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(conversation));

        var result = enabledService().getOrCreateConversation(USER);

        assertThat(result).isSameAs(conversation);
        verify(conversationRepository, never()).insertOpenConversationIfAbsent(any(), any());
    }

    @Test
    @DisplayName("Get or create inserts one permanent conversation when absent")
    void getOrCreateConversation_absent_insertsPermanentConversation() {
        SupportConversationEntity conversation = conversation();
        doNothing().when(availabilityService).requireAvailable(USER);
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
        doThrow(new SupportChatEmailVerificationRequiredException())
                .when(availabilityService)
                .requireAvailable(USER);

        assertThatThrownBy(() -> enabledService().getOrCreateConversation(USER))
                .isInstanceOf(SupportChatEmailVerificationRequiredException.class);
    }

    @Test
    @DisplayName("Restricted user cannot create a conversation")
    void getOrCreateConversation_accessRestricted_throwsForbidden() {
        doThrow(new SupportChatAccessRestrictedException())
                .when(availabilityService)
                .requireAvailable(USER);

        assertThatThrownBy(() -> enabledService().getOrCreateConversation(USER))
                .isInstanceOf(SupportChatAccessRestrictedException.class);
    }

    @Test
    @DisplayName("Sending message persists before owner delivery and marks delivered")
    void sendCustomerMessage_valid_persistsAndSendsToOwner() {
        SupportConversationEntity conversation = conversation();
        SupportMessageEntity saved = savedCustomerMessage("Hello support");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(messageRepository.updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.SENT, false))
                .thenReturn(1);
        when(ownerMessageSender.send(any(OwnerMessage.class))).thenReturn(OwnerMessageDeliveryResult.deliveredResult());

        var result = enabledService()
                .sendCustomerMessage(
                        USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "  Hello   support  ", null, "203.0.113.10");

        assertThat(result.getDeliveryStatus()).isEqualTo(SupportMessageDeliveryStatus.SENT);
        ArgumentCaptor<SupportMessageEntity> messageCaptor = ArgumentCaptor.forClass(SupportMessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getBody()).isEqualTo("Hello   support");
        assertThat(messageCaptor.getValue().getNormalizedBody()).isEqualTo("hello support");
        verify(conversationRepository).touchLastMessageAt(CONVERSATION_ID);
        verify(ownerMessageSender).send(any(OwnerMessage.class));
        var order = inOrder(transactionManager, ownerMessageSender, messageRepository);
        order.verify(transactionManager).commit(any());
        order.verify(ownerMessageSender).send(any(OwnerMessage.class));
        order.verify(messageRepository).updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.SENT, false);
    }

    @Test
    @DisplayName("Owner delivery exception stores failed message and returns generic support failure")
    void sendCustomerMessage_ownerSenderThrows_marksFailedAndThrowsGenericFailure() {
        SupportMessageEntity saved = savedCustomerMessage("Hello");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(messageRepository.updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.FAILED, true))
                .thenReturn(1);
        when(ownerMessageSender.send(any(OwnerMessage.class)))
                .thenThrow(new IllegalStateException("owner unavailable"));

        assertThatThrownBy(() -> enabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null, "203.0.113.10"))
                .isInstanceOf(SupportChatOwnerDeliveryFailedException.class);

        assertThat(saved.getDeliveryStatus()).isEqualTo(SupportMessageDeliveryStatus.FAILED);
        assertThat(saved.isOperatorInspectionRequired()).isTrue();
        verify(conversationRepository).touchLastMessageAt(CONVERSATION_ID);
        verify(messageRepository).updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.FAILED, true);
    }

    @Test
    @DisplayName("Owner reply is persisted as visible sent message with Telegram correlation")
    void saveOwnerReply_valid_persistsVisibleOwnerMessage() {
        SupportConversationEntity conversation = conversation();
        SupportMessageEntity saved = savedOwnerMessage();
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
        verify(messagePublisher).publishOwnerReply(conversation, saved);
    }

    @Test
    @DisplayName("Duplicate Telegram owner reply is ignored without persistence")
    void saveOwnerReply_duplicateTelegramUpdate_ignoresMessage() {
        when(messageRepository.existsByTelegramUpdateId(9001L)).thenReturn(true);

        var result = enabledService().saveOwnerReply(conversation(), "Owner answer", 9001L, 7001L);

        assertThat(result).isEmpty();
        verify(messageRepository, never()).save(any());
        verify(conversationRepository, never()).touchLastMessageAt(any());
        verifyNoInteractions(messagePublisher);
    }

    @Test
    @DisplayName("Disabled support chat rejects message before persistence")
    void sendCustomerMessage_disabled_throwsNotFound() {
        doThrow(new SupportChatDisabledException()).when(availabilityService).requireAvailable(USER);

        assertThatThrownBy(() -> disabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null, "203.0.113.10"))
                .isInstanceOf(SupportChatDisabledException.class);

        verify(messageRepository, never()).save(any());
        verify(ownerMessageSender, never()).send(any());
    }

    @Test
    @DisplayName("Blank message is rejected before rate limiting and persistence")
    void sendCustomerMessage_blankBody_throwsBadRequest() {
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() -> enabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "   ", null, "203.0.113.10"))
                .isInstanceOf(InvalidSupportChatMessageException.class);

        verify(rateLimiter, never()).tryConsume(any(), anyInt(), any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Messages for another user's conversation are rejected")
    void sendCustomerMessage_foreignConversation_throwsNotFound() {
        SupportConversationEntity conversation = conversation();
        conversation.setUserId(UUID.randomUUID());
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> enabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null, "203.0.113.10"))
                .isInstanceOf(SupportChatConversationNotFoundException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sending with same client message ID is idempotent")
    void sendCustomerMessage_existingClientMessageId_returnsExistingMessage() {
        SupportMessageEntity existing = savedCustomerMessage("Already accepted");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.of(existing));

        var result = enabledService()
                .sendCustomerMessage(
                        USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Already accepted", null, "203.0.113.10");

        assertThat(result).isSameAs(existing);
        verify(messageRepository, never()).save(any());
        verify(ownerMessageSender, never()).send(any());
    }

    @Test
    @DisplayName("Repeated identical customer message is rejected")
    void sendCustomerMessage_repeatedIdenticalBody_throwsConflict() {
        SupportMessageEntity previous = savedCustomerMessage("Hello");
        previous.setNormalizedBody("hello");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> enabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, " hello ", null, "203.0.113.10"))
                .isInstanceOf(DuplicateSupportChatMessageException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repeated identical customer message is allowed after previous delivery failed")
    void sendCustomerMessage_repeatedIdenticalBodyAfterFailedDelivery_allowsRetry() {
        SupportMessageEntity previous = savedCustomerMessage("Hello");
        previous.setNormalizedBody("hello");
        previous.setDeliveryStatus(SupportMessageDeliveryStatus.FAILED);
        SupportMessageEntity saved = savedCustomerMessage("Hello");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.of(previous));
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(messageRepository.updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.SENT, false))
                .thenReturn(1);
        when(ownerMessageSender.send(any(OwnerMessage.class))).thenReturn(OwnerMessageDeliveryResult.deliveredResult());

        var result = enabledService()
                .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, " hello ", null, "203.0.113.10");

        assertThat(result.getDeliveryStatus()).isEqualTo(SupportMessageDeliveryStatus.SENT);
    }

    @Test
    @DisplayName("Rate-limited message is rejected before persistence")
    void sendCustomerMessage_rateLimited_throwsTooManyRequests() {
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(blockedRateLimit());

        assertThatThrownBy(() -> enabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null, "203.0.113.10"))
                .isInstanceOf(SupportChatRateLimitExceededException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("IP rate limit is enforced before persistence")
    void sendCustomerMessage_ipRateLimited_throwsTooManyRequests() {
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(allowedRateLimit())
                .thenReturn(allowedRateLimit())
                .thenReturn(allowedRateLimit())
                .thenReturn(allowedRateLimit())
                .thenReturn(blockedRateLimit());

        assertThatThrownBy(() -> enabledService()
                        .sendCustomerMessage(USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", null, "203.0.113.10"))
                .isInstanceOf(SupportChatRateLimitExceededException.class);
        verify(rateLimiter).tryConsume(eq("support-chat:ip-minute:203.0.113.10"), eq(60), any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("First customer message verifies Turnstile when enabled")
    void sendCustomerMessage_firstMessageWithTurnstileEnabled_verifiesToken() {
        SupportMessageEntity saved = savedCustomerMessage("Hello");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(messageRepository.updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.SENT, false))
                .thenReturn(1);
        when(ownerMessageSender.send(any(OwnerMessage.class))).thenReturn(OwnerMessageDeliveryResult.deliveredResult());

        turnstileEnabledService()
                .sendCustomerMessage(
                        USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello", "turnstile-token", "203.0.113.10");

        verify(turnstileVerifier).verify("turnstile-token");
    }

    @Test
    @DisplayName("Message after long inactivity verifies Turnstile when enabled")
    void sendCustomerMessage_longInactiveConversationWithTurnstileEnabled_verifiesToken() {
        SupportMessageEntity previous = savedCustomerMessage("Old message");
        previous.setCreatedAt(OffsetDateTime.now().minusDays(2));
        SupportMessageEntity saved = savedCustomerMessage("Hello again");
        doNothing().when(availabilityService).requireAvailable(USER);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByConversationIdAndClientMessageId(CONVERSATION_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(
                        CONVERSATION_ID, SupportMessageSenderType.CUSTOMER))
                .thenReturn(Optional.of(previous));
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(allowedRateLimit());
        when(messageRepository.save(any(SupportMessageEntity.class))).thenReturn(saved);
        when(messageRepository.updateDeliveryStatus(saved.getId(), SupportMessageDeliveryStatus.SENT, false))
                .thenReturn(1);
        when(ownerMessageSender.send(any(OwnerMessage.class))).thenReturn(OwnerMessageDeliveryResult.deliveredResult());

        turnstileEnabledService()
                .sendCustomerMessage(
                        USER, CONVERSATION_ID, CLIENT_MESSAGE_ID, "Hello again", "turnstile-token", "203.0.113.10");

        verify(turnstileVerifier).verify("turnstile-token");
    }

    @Test
    @DisplayName("History uses bounded page size and retention window")
    void getHistory_usesBoundedPageSizeAndRetentionWindow() {
        doNothing().when(availabilityService).requireAvailable(USER);
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
        SupportChatProperties supportChatProperties = properties(true);
        return new SupportChatService(
                supportChatProperties,
                availabilityService,
                eligibilityService,
                conversationRepository,
                messageRepository,
                new SupportChatMessageService(
                        supportChatProperties,
                        availabilityService,
                        conversationRepository,
                        messageRepository,
                        ownerMessageSender,
                        messagePublisher,
                        turnstileVerifier,
                        new SupportChatAbuseGuard(supportChatProperties, Clock.systemUTC()),
                        rateLimiter,
                        transactionManager));
    }

    private SupportChatService disabledService() {
        SupportChatProperties supportChatProperties = properties(false);
        return new SupportChatService(
                supportChatProperties,
                availabilityService,
                eligibilityService,
                conversationRepository,
                messageRepository,
                new SupportChatMessageService(
                        supportChatProperties,
                        availabilityService,
                        conversationRepository,
                        messageRepository,
                        ownerMessageSender,
                        messagePublisher,
                        turnstileVerifier,
                        new SupportChatAbuseGuard(supportChatProperties, Clock.systemUTC()),
                        rateLimiter,
                        transactionManager));
    }

    private SupportChatService turnstileEnabledService() {
        SupportChatProperties supportChatProperties = properties(true, true);
        return new SupportChatService(
                supportChatProperties,
                availabilityService,
                eligibilityService,
                conversationRepository,
                messageRepository,
                new SupportChatMessageService(
                        supportChatProperties,
                        availabilityService,
                        conversationRepository,
                        messageRepository,
                        ownerMessageSender,
                        messagePublisher,
                        turnstileVerifier,
                        new SupportChatAbuseGuard(supportChatProperties, Clock.systemUTC()),
                        rateLimiter,
                        transactionManager));
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
                new Turnstile(turnstileEnabled, Duration.ofHours(24), Duration.ofMinutes(5)),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)),
                        new Bucket(60, Duration.ofMinutes(1))),
                Set.of());
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

    private static SupportMessageEntity savedOwnerMessage() {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversationId(CONVERSATION_ID);
        message.setSenderType(SupportMessageSenderType.OWNER);
        message.setBody("Owner answer");
        message.setNormalizedBody("owner answer");
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
