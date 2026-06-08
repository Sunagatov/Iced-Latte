package com.zufar.icedlatte.supportchat.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Bucket;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.RateLimits;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.service.SupportChatEligibility;
import com.zufar.icedlatte.supportchat.service.SupportChatEligibilityService;

@DisplayName("SupportChatSubscriptionAuthorizationInterceptor unit tests")
class SupportChatSubscriptionAuthorizationInterceptorTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    private final SupportChatEligibilityService eligibilityService = mock(SupportChatEligibilityService.class);
    private final SupportConversationRepository conversationRepository = mock(SupportConversationRepository.class);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    @DisplayName("Allows owner of verified user's support chat conversation to subscribe")
    void preSend_supportChatSubscriptionOwnedByEligibleUser_allowsSubscription() {
        SupportConversationEntity conversation = conversation(USER_ID);
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        Message<?> message = subscriptionMessage(
                SupportChatWebSocketDestinations.conversationMessages(CONVERSATION_ID), new PrincipalUser(USER_ID));

        var result = enabledInterceptor().preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("Rejects support chat subscription for another user's conversation")
    void preSend_supportChatSubscriptionForForeignConversation_rejectsSubscription() {
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.createEligible());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation(UUID.randomUUID())));

        Message<?> message = subscriptionMessage(
                SupportChatWebSocketDestinations.conversationMessages(CONVERSATION_ID), new PrincipalUser(USER_ID));

        assertThatThrownBy(() -> enabledInterceptor().preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Rejects support chat subscription when user is not eligible")
    void preSend_supportChatSubscriptionForUnverifiedUser_rejectsSubscription() {
        when(eligibilityService.eligibilityFor(USER_ID)).thenReturn(SupportChatEligibility.emailVerificationRequired());

        Message<?> message = subscriptionMessage(
                SupportChatWebSocketDestinations.conversationMessages(CONVERSATION_ID), new PrincipalUser(USER_ID));

        assertThatThrownBy(() -> enabledInterceptor().preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    @DisplayName("Ignores subscriptions outside support chat destinations")
    void preSend_otherSubscription_ignoresDestination() {
        Message<?> message = subscriptionMessage("/topic/products", new PrincipalUser(USER_ID));

        var result = enabledInterceptor().preSend(message, channel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(eligibilityService, conversationRepository);
    }

    @Test
    @DisplayName("Rejects malformed subscriptions inside support chat destination namespace")
    void preSend_malformedSupportChatSubscription_rejectsSubscription() {
        Message<?> message =
                subscriptionMessage("/topic/support-chat/conversations/not-a-uuid", new PrincipalUser(USER_ID));

        assertThatThrownBy(() -> enabledInterceptor().preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(eligibilityService, conversationRepository);
    }

    private SupportChatSubscriptionAuthorizationInterceptor enabledInterceptor() {
        return new SupportChatSubscriptionAuthorizationInterceptor(
                enabledProperties(), eligibilityService, conversationRepository);
    }

    private static Message<?> subscriptionMessage(String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static SupportConversationEntity conversation(UUID userId) {
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(CONVERSATION_ID);
        conversation.setUserId(userId);
        return conversation;
    }

    private static SupportChatProperties enabledProperties() {
        return new SupportChatProperties(
                true,
                4000,
                90,
                OwnerMessageMode.FAKE,
                new Telegram("", "", 0L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5)),
                new Turnstile(false, Duration.ofHours(24), Duration.ofMinutes(5)),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)),
                        new Bucket(60, Duration.ofMinutes(1))));
    }

    private record PrincipalUser(UUID userId) implements Principal, Identifiable {
        @Override
        public String getName() {
            return userId.toString();
        }

        @Override
        public UUID getId() {
            return userId;
        }
    }
}
