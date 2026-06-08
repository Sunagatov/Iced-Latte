package com.zufar.icedlatte.supportchat.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Bucket;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.RateLimits;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.service.SupportChatService;

@DisplayName("TelegramWebhookService unit tests")
class TelegramWebhookServiceTest {

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private final SupportConversationRepository conversationRepository = mock(SupportConversationRepository.class);
    private final SupportChatService supportChatService = mock(SupportChatService.class);

    @Test
    @DisplayName("Owner reply in forum topic is persisted")
    void handle_topicOwnerReply_persistsMessage() {
        SupportConversationEntity conversation = conversation();
        when(conversationRepository.findByTelegramMessageThreadId(456L)).thenReturn(Optional.of(conversation));

        var result = enabledService().handle("secret", update(9001L, topicMessage("Owner answer")));

        assertThat(result).isEqualTo(TelegramWebhookResult.PROCESSED);
        verify(supportChatService).saveOwnerReply(conversation, "Owner answer", 9001L, 7001L);
    }

    @Test
    @DisplayName("Owner reply to fallback bot message is persisted")
    void handle_fallbackOwnerReply_persistsMessage() {
        SupportConversationEntity conversation = conversation();
        TelegramWebhookUpdate.TelegramWebhookMessage message = message(7002L, null, "Fallback answer", 100L);
        when(conversationRepository.findByTelegramFallbackMessageId(100L)).thenReturn(Optional.of(conversation));

        var result = enabledService().handle("secret", update(9002L, message));

        assertThat(result).isEqualTo(TelegramWebhookResult.PROCESSED);
        verify(supportChatService).saveOwnerReply(conversation, "Fallback answer", 9002L, 7002L);
    }

    @Test
    @DisplayName("Invalid webhook secret is unauthorized")
    void handle_invalidSecret_rejectsRequest() {
        var result = enabledService().handle("wrong", update(9001L, topicMessage("Owner answer")));

        assertThat(result).isEqualTo(TelegramWebhookResult.UNAUTHORIZED);
        verifyNoInteractions(conversationRepository, supportChatService);
    }

    @Test
    @DisplayName("Wrong owner is ignored")
    void handle_wrongOwner_ignoresUpdate() {
        TelegramWebhookUpdate.TelegramWebhookMessage message = new TelegramWebhookUpdate.TelegramWebhookMessage(
                7001L,
                456L,
                new TelegramWebhookUpdate.TelegramWebhookChat(-1001234567890L),
                new TelegramWebhookUpdate.TelegramWebhookUser(999L, false),
                "Owner answer",
                null);

        var result = enabledService().handle("secret", update(9001L, message));

        assertThat(result).isEqualTo(TelegramWebhookResult.IGNORED);
        verifyNoInteractions(conversationRepository, supportChatService);
    }

    @Test
    @DisplayName("Wrong chat is ignored")
    void handle_wrongChat_ignoresUpdate() {
        TelegramWebhookUpdate.TelegramWebhookMessage message = new TelegramWebhookUpdate.TelegramWebhookMessage(
                7001L,
                456L,
                new TelegramWebhookUpdate.TelegramWebhookChat(-1009999999999L),
                new TelegramWebhookUpdate.TelegramWebhookUser(555L, false),
                "Owner answer",
                null);

        var result = enabledService().handle("secret", update(9001L, message));

        assertThat(result).isEqualTo(TelegramWebhookResult.IGNORED);
        verifyNoInteractions(conversationRepository, supportChatService);
    }

    @Test
    @DisplayName("Non-text message is ignored")
    void handle_nonText_ignoresUpdate() {
        var result = enabledService().handle("secret", update(9001L, topicMessage(null)));

        assertThat(result).isEqualTo(TelegramWebhookResult.IGNORED);
        verifyNoInteractions(conversationRepository, supportChatService);
    }

    @Test
    @DisplayName("Disabled support chat ignores valid Telegram update")
    void handle_disabledFeature_ignoresUpdate() {
        var result = disabledService().handle("secret", update(9001L, topicMessage("Owner answer")));

        assertThat(result).isEqualTo(TelegramWebhookResult.IGNORED);
        verifyNoInteractions(conversationRepository, supportChatService);
    }

    @Test
    @DisplayName("Duplicate Telegram update race is ignored")
    void handle_duplicateUpdateRace_ignoresUpdate() {
        SupportConversationEntity conversation = conversation();
        when(conversationRepository.findByTelegramMessageThreadId(456L)).thenReturn(Optional.of(conversation));
        doThrow(new DataIntegrityViolationException("duplicate telegram update"))
                .when(supportChatService)
                .saveOwnerReply(conversation, "Owner answer", 9001L, 7001L);

        var result = enabledService().handle("secret", update(9001L, topicMessage("Owner answer")));

        assertThat(result).isEqualTo(TelegramWebhookResult.IGNORED);
    }

    private TelegramWebhookService enabledService() {
        return new TelegramWebhookService(properties(true), conversationRepository, supportChatService);
    }

    private TelegramWebhookService disabledService() {
        return new TelegramWebhookService(properties(false), conversationRepository, supportChatService);
    }

    private static TelegramWebhookUpdate update(long updateId, TelegramWebhookUpdate.TelegramWebhookMessage message) {
        return new TelegramWebhookUpdate(updateId, message);
    }

    private static TelegramWebhookUpdate.TelegramWebhookMessage topicMessage(String text) {
        return message(7001L, 456L, text, null);
    }

    private static TelegramWebhookUpdate.TelegramWebhookMessage message(
            long messageId, Long messageThreadId, String text, Long replyToMessageId) {
        TelegramWebhookUpdate.TelegramWebhookMessage replyToMessage = replyToMessageId == null
                ? null
                : new TelegramWebhookUpdate.TelegramWebhookMessage(replyToMessageId, null, null, null, null, null);
        return new TelegramWebhookUpdate.TelegramWebhookMessage(
                messageId,
                messageThreadId,
                new TelegramWebhookUpdate.TelegramWebhookChat(-1001234567890L),
                new TelegramWebhookUpdate.TelegramWebhookUser(555L, false),
                text,
                replyToMessage);
    }

    private static SupportConversationEntity conversation() {
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(CONVERSATION_ID);
        conversation.setUserId(USER_ID);
        return conversation;
    }

    private static SupportChatProperties properties(boolean enabled) {
        return new SupportChatProperties(
                enabled,
                4000,
                90,
                OwnerMessageMode.TELEGRAM,
                new Telegram(
                        "bot-token",
                        "-1001234567890",
                        555L,
                        "secret",
                        true,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)),
                new Turnstile(false),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10))));
    }
}
