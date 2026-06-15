package com.zufar.icedlatte.supportchat.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.service.SupportChatAvailabilityService;

@DisplayName("SupportChatWebSocketSessionLifecycleListener unit tests")
class SupportChatWebSocketSessionLifecycleListenerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Test
    @DisplayName("Releases a support chat session slot after transport disconnect")
    void onDisconnect_supportChatTransportDisconnect_releasesSessionSlot() {
        SupportChatAvailabilityService availabilityService = mock(SupportChatAvailabilityService.class);
        SupportConversationRepository conversationRepository = mock(SupportConversationRepository.class);
        MessageChannel channel = mock(MessageChannel.class);
        SupportChatWebSocketSessionRegistry sessionRegistry = new SupportChatWebSocketSessionRegistry(1);
        SupportChatSubscriptionAuthorizationInterceptor interceptor =
                new SupportChatSubscriptionAuthorizationInterceptor(
                        availabilityService, conversationRepository, sessionRegistry);
        SupportChatWebSocketSessionLifecycleListener listener =
                new SupportChatWebSocketSessionLifecycleListener(sessionRegistry);
        when(availabilityService.isEligible(USER_ID)).thenReturn(true);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        interceptor.preSend(subscriptionMessage("session-1"), channel);

        assertThat(rejectedSubscription(interceptor, channel)).isTrue();

        listener.onDisconnect(disconnectEvent());

        Message<?> result = interceptor.preSend(subscriptionMessage("session-2"), channel);

        assertThat(result).isNotNull();
    }

    private static boolean rejectedSubscription(
            SupportChatSubscriptionAuthorizationInterceptor interceptor, MessageChannel channel) {
        try {
            interceptor.preSend(subscriptionMessage("session-2"), channel);
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    private static Message<?> subscriptionMessage(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(SupportChatWebSocketDestinations.conversationMessages(CONVERSATION_ID));
        accessor.setUser(new PrincipalUser(USER_ID));
        accessor.setSessionId(sessionId);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static SessionDisconnectEvent disconnectEvent() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId("session-1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(new Object(), message, "session-1", CloseStatus.NORMAL);
    }

    private static SupportConversationEntity conversation() {
        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(CONVERSATION_ID);
        conversation.setUserId(USER_ID);
        return conversation;
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
