package com.zufar.icedlatte.supportchat.realtime;

import static com.zufar.icedlatte.supportchat.realtime.SupportChatWebSocketDestinations.CONVERSATION_MESSAGES_PREFIX;
import static com.zufar.icedlatte.supportchat.realtime.SupportChatWebSocketDestinations.CONVERSATION_MESSAGES_SUFFIX;
import static com.zufar.icedlatte.supportchat.realtime.SupportChatWebSocketDestinations.SUPPORT_CHAT_TOPIC_PREFIX;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.service.SupportChatEligibilityService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupportChatSubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private final SupportChatProperties properties;
    private final SupportChatEligibilityService eligibilityService;
    private final SupportConversationRepository conversationRepository;
    private final SupportChatWebSocketSessionRegistry sessionRegistry;

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            unregisterSession(accessor);
        }
        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Optional<UUID> conversationId = conversationId(destination);
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied.");
        if (conversationId.isEmpty()) {
            if (isSupportChatDestination(destination)) {
                throw accessDeniedException;
            }
            return;
        }

        UUID userId = userId(accessor.getUser()).orElseThrow(() -> accessDeniedException);
        if (!properties.enabled() || !eligibilityService.eligibilityFor(userId).eligible()) {
            throw accessDeniedException;
        }

        boolean ownsConversation = conversationRepository
                .findById(conversationId.get())
                .filter(conversation -> conversation.getUserId().equals(userId))
                .isPresent();
        if (!ownsConversation) {
            throw accessDeniedException;
        }
        if (!sessionRegistry.register(userId, sessionId(accessor))) {
            throw accessDeniedException;
        }
    }

    private void unregisterSession(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessionRegistry.unregister(sessionId);
        }
    }

    private static Optional<UUID> conversationId(@Nullable String destination) {
        if (destination == null
                || !destination.startsWith(CONVERSATION_MESSAGES_PREFIX)
                || !destination.endsWith(CONVERSATION_MESSAGES_SUFFIX)) {
            return Optional.empty();
        }

        int prefixLength = CONVERSATION_MESSAGES_PREFIX.length();
        int suffixStart = destination.length() - CONVERSATION_MESSAGES_SUFFIX.length();
        try {
            return Optional.of(UUID.fromString(destination.substring(prefixLength, suffixStart)));
        } catch (IllegalArgumentException _) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    private static boolean isSupportChatDestination(@Nullable String destination) {
        return destination != null && destination.startsWith(SUPPORT_CHAT_TOPIC_PREFIX);
    }

    private static Optional<UUID> userId(@Nullable Principal principal) {
        return switch (principal) {
            case Identifiable identifiable -> Optional.of(identifiable.getId());
            case Authentication authentication when authentication.getPrincipal() instanceof Identifiable identifiable -> Optional.of(identifiable.getId());
            case null, default -> Optional.empty();
        };
    }

    private static String sessionId(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new AccessDeniedException("Access denied.");
        }
        return sessionId;
    }
}
