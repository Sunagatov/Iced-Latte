package com.zufar.icedlatte.supportchat.realtime;

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

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Optional<UUID> conversationId = conversationId(destination);
        if (conversationId.isEmpty()) {
            if (isSupportChatDestination(destination)) {
                throw new AccessDeniedException("Access denied.");
            }
            return;
        }

        UUID userId = userId(accessor.getUser()).orElseThrow(() -> new AccessDeniedException("Access denied."));
        if (!properties.enabled() || !eligibilityService.eligibilityFor(userId).eligible()) {
            throw new AccessDeniedException("Access denied.");
        }

        boolean ownsConversation = conversationRepository
                .findById(conversationId.get())
                .filter(conversation -> conversation.getUserId().equals(userId))
                .isPresent();
        if (!ownsConversation) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    private static Optional<UUID> conversationId(@Nullable String destination) {
        if (destination == null
                || !destination.startsWith(SupportChatWebSocketDestinations.CONVERSATION_MESSAGES_PREFIX)
                || !destination.endsWith(SupportChatWebSocketDestinations.CONVERSATION_MESSAGES_SUFFIX)) {
            return Optional.empty();
        }

        int prefixLength = SupportChatWebSocketDestinations.CONVERSATION_MESSAGES_PREFIX.length();
        int suffixStart = destination.length() - SupportChatWebSocketDestinations.CONVERSATION_MESSAGES_SUFFIX.length();
        try {
            return Optional.of(UUID.fromString(destination.substring(prefixLength, suffixStart)));
        } catch (IllegalArgumentException _) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    private static boolean isSupportChatDestination(@Nullable String destination) {
        return destination != null
                && destination.startsWith(SupportChatWebSocketDestinations.SUPPORT_CHAT_TOPIC_PREFIX);
    }

    private static Optional<UUID> userId(@Nullable Principal principal) {
        if (principal instanceof Identifiable identifiable) {
            return Optional.of(identifiable.getId());
        }
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof Identifiable identifiable) {
            return Optional.of(identifiable.getId());
        }
        return Optional.empty();
    }
}
