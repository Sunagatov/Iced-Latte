package com.zufar.icedlatte.supportchat.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Bucket;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.RateLimits;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;

@DisplayName("TelegramOwnerMessageSender unit tests")
class TelegramOwnerMessageSenderTest {

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final OwnerMessage OWNER_MESSAGE =
            new OwnerMessage(CONVERSATION_ID, MESSAGE_ID, "Olivia Stone", "customer@example.com", "Hello support");

    private final SupportConversationRepository conversationRepository = mock(SupportConversationRepository.class);
    private final FakeTelegramBotClient telegramBotClient = new FakeTelegramBotClient();
    private final TelegramOwnerMessageFormatter formatter = new TelegramOwnerMessageFormatter();

    @Test
    @DisplayName("Existing Telegram topic is reused")
    void send_existingTopic_sendsToThread() {
        SupportConversationEntity conversation = conversation();
        conversation.setTelegramMessageThreadId(123L);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(telegramBotClient.createForumTopicCalls).isZero();
        assertThat(telegramBotClient.lastThreadId).isEqualTo(123L);
        assertThat(telegramBotClient.lastText)
                .contains("Customer: Olivia Stone")
                .contains("Email: customer@example.com")
                .contains("Reply in this topic or reply to this bot message.")
                .doesNotContain("User ID:");
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fallback message is sent when existing topic delivery fails")
    void send_existingTopicDeliveryFails_sendsFallbackMessage() {
        SupportConversationEntity conversation = conversation();
        conversation.setTelegramMessageThreadId(123L);
        telegramBotClient.nextMessages.add(null);
        telegramBotClient.nextMessages.add(new TelegramMessageRef(789L));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(conversation.getTelegramMessageThreadId()).isEqualTo(123L);
        assertThat(conversation.getTelegramFallbackMessageId()).isEqualTo(789L);
        assertThat(telegramBotClient.sendMessageCalls).isEqualTo(2);
        assertThat(telegramBotClient.lastThreadId).isNull();
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("New Telegram forum topic is created and stored when possible")
    void send_withoutTopic_createsTopicAndStoresCorrelation() {
        SupportConversationEntity conversation = conversation();
        telegramBotClient.nextTopic = new TelegramForumTopic(456L);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(conversation.getTelegramMessageThreadId()).isEqualTo(456L);
        assertThat(telegramBotClient.lastThreadId).isEqualTo(456L);
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Fallback message is sent and stored when topic creation fails")
    void send_topicCreationFails_sendsFallbackMessage() {
        SupportConversationEntity conversation = conversation();
        telegramBotClient.nextTopic = null;
        telegramBotClient.nextMessage = new TelegramMessageRef(789L);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(conversation.getTelegramMessageThreadId()).isNull();
        assertThat(conversation.getTelegramFallbackMessageId()).isEqualTo(789L);
        assertThat(telegramBotClient.lastThreadId).isNull();
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Latest fallback message replaces previous fallback correlation")
    void send_existingFallback_sendsFallbackMessageAndStoresLatestCorrelation() {
        SupportConversationEntity conversation = conversation();
        conversation.setTelegramFallbackMessageId(100L);
        telegramBotClient.nextMessage = new TelegramMessageRef(101L);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(false).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(conversation.getTelegramFallbackMessageId()).isEqualTo(101L);
        assertThat(telegramBotClient.lastThreadId).isNull();
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Fallback message is sent when new topic delivery fails")
    void send_newTopicDeliveryFails_sendsFallbackMessage() {
        SupportConversationEntity conversation = conversation();
        telegramBotClient.nextMessages.add(null);
        telegramBotClient.nextMessages.add(new TelegramMessageRef(790L));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(conversation.getTelegramMessageThreadId()).isEqualTo(200L);
        assertThat(conversation.getTelegramFallbackMessageId()).isEqualTo(790L);
        assertThat(telegramBotClient.sendMessageCalls).isEqualTo(2);
        verify(conversationRepository, times(2)).save(conversation);
    }

    @Test
    @DisplayName("Delivery fails when topic and fallback sending both fail")
    void send_telegramUnavailable_returnsFailedResult() {
        SupportConversationEntity conversation = conversation();
        telegramBotClient.nextTopic = null;
        telegramBotClient.nextMessage = null;
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isFalse();
        assertThat(conversation.getTelegramMessageThreadId()).isNull();
        assertThat(conversation.getTelegramFallbackMessageId()).isNull();
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Created topic correlation is stored when topic and fallback delivery fail")
    void send_newTopicDeliveryAndFallbackFail_storesCreatedTopicCorrelation() {
        SupportConversationEntity conversation = conversation();
        telegramBotClient.nextTopic = new TelegramForumTopic(456L);
        telegramBotClient.nextMessages.add(null);
        telegramBotClient.nextMessage = null;
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isFalse();
        assertThat(conversation.getTelegramMessageThreadId()).isEqualTo(456L);
        assertThat(conversation.getTelegramFallbackMessageId()).isNull();
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Forum topic creation can be disabled")
    void send_forumTopicsDisabled_sendsFallbackDirectly() {
        SupportConversationEntity conversation = conversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        var result = sender(false).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isTrue();
        assertThat(telegramBotClient.createForumTopicCalls).isZero();
        assertThat(conversation.getTelegramFallbackMessageId()).isEqualTo(100L);
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Missing conversation fails without Telegram call")
    void send_missingConversation_returnsFailedResult() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

        var result = sender(true).send(OWNER_MESSAGE);

        assertThat(result.delivered()).isFalse();
        assertThat(telegramBotClient.sendMessageCalls).isZero();
    }

    private TelegramOwnerMessageSender sender(boolean forumTopicsEnabled) {
        return new TelegramOwnerMessageSender(
                properties(forumTopicsEnabled), conversationRepository, telegramBotClient, formatter);
    }

    private static SupportConversationEntity conversation() {
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(CONVERSATION_ID);
        conversation.setUserId(UUID.randomUUID());
        return conversation;
    }

    private static SupportChatProperties properties(boolean forumTopicsEnabled) {
        return new SupportChatProperties(
                true,
                4000,
                90,
                OwnerMessageMode.TELEGRAM,
                new Telegram(
                        "bot-token",
                        "-1001234567890",
                        555L,
                        "webhook-secret",
                        forumTopicsEnabled,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)),
                new Turnstile(false, Duration.ofHours(24), Duration.ofMinutes(5)),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)),
                        new Bucket(60, Duration.ofMinutes(1))));
    }

    private static final class FakeTelegramBotClient implements TelegramBotClient {

        private TelegramForumTopic nextTopic = new TelegramForumTopic(200L);
        private TelegramMessageRef nextMessage = new TelegramMessageRef(100L);
        private final List<TelegramMessageRef> nextMessages = new ArrayList<>();
        private int createForumTopicCalls;
        private int sendMessageCalls;
        private Long lastThreadId;
        private String lastText;

        @Override
        public Optional<TelegramForumTopic> createForumTopic(String name) {
            createForumTopicCalls++;
            return Optional.ofNullable(nextTopic);
        }

        @Override
        public Optional<TelegramMessageRef> sendMessage(Long messageThreadId, String text) {
            sendMessageCalls++;
            lastThreadId = messageThreadId;
            lastText = text;
            if (sendMessageCalls <= nextMessages.size()) {
                return Optional.ofNullable(nextMessages.get(sendMessageCalls - 1));
            }
            return Optional.ofNullable(nextMessage);
        }
    }
}
