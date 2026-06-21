package com.zufar.icedlatte.supportchat.service;

import static com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus.SENT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.common.monitoring.AbuseSignalRecorder;
import com.zufar.icedlatte.common.turnstile.TurnstileVerificationException;
import com.zufar.icedlatte.common.turnstile.TurnstileVerificationRequest;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.ratelimit.api.RateLimitResult;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.exception.DuplicateSupportChatMessageException;
import com.zufar.icedlatte.supportchat.exception.SupportChatRateLimitExceededException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupportChatCustomerMessagePolicy unit tests")
class SupportChatCustomerMessagePolicyTest {

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private SupportChatAbuseGuard abuseGuard;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private AbuseSignalRecorder abuseSignalRecorder;

    private SupportChatCustomerMessagePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SupportChatCustomerMessagePolicy(
                supportChatProperties(), turnstileVerifier, abuseGuard, rateLimiter, abuseSignalRecorder);
        lenient()
                .when(rateLimiter.tryConsume(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 10, 9, 0, 60));
    }

    @Test
    @DisplayName("records duplicate rejection abuse signal")
    void enforceCustomerMessageRules_duplicateMessage_recordsSignal() {
        UUID conversationId = UUID.randomUUID();
        var previousMessage = previousCustomerMessage(conversationId, "same");
        var content = new SupportChatMessageBodyPolicy.MessageContent("same", "same");

        assertThatThrownBy(() -> policy.enforceCustomerMessageRules(
                        UUID.randomUUID(), conversationId, "203.0.113.10", content, previousMessage, null))
                .isInstanceOf(DuplicateSupportChatMessageException.class);

        verify(abuseGuard).requireTurnstileForNextMessage(conversationId);
        verify(abuseSignalRecorder).record("support-chat", "duplicate_rejected");
    }

    @Test
    @DisplayName("records rate-limited abuse signal")
    void enforceCustomerMessageRules_rateLimited_recordsSignal() {
        UUID conversationId = UUID.randomUUID();
        when(rateLimiter.tryConsume(any(), anyInt(), any())).thenReturn(new RateLimitResult(false, 20, 0, 0, 60));

        assertThatThrownBy(() -> policy.enforceCustomerMessageRules(
                        UUID.randomUUID(),
                        conversationId,
                        "203.0.113.10",
                        new SupportChatMessageBodyPolicy.MessageContent("hello", "hello"),
                        null,
                        null))
                .isInstanceOf(SupportChatRateLimitExceededException.class);

        verify(abuseGuard).requireTurnstileForNextMessage(conversationId);
        verify(abuseSignalRecorder).record("support-chat", "rate_limited");
    }

    @Test
    @DisplayName("records failed Turnstile abuse signal")
    void enforceCustomerMessageRules_turnstileFailure_recordsSignal() {
        UUID conversationId = UUID.randomUUID();
        when(abuseGuard.requiresTurnstile(null, conversationId)).thenReturn(true);
        doThrow(new TurnstileVerificationException("Turnstile verification failed"))
                .when(turnstileVerifier)
                .verify(any(TurnstileVerificationRequest.class));

        assertThatThrownBy(() -> policy.enforceCustomerMessageRules(
                        UUID.randomUUID(),
                        conversationId,
                        "203.0.113.10",
                        new SupportChatMessageBodyPolicy.MessageContent("hello", "hello"),
                        null,
                        "bad-token"))
                .isInstanceOf(TurnstileVerificationException.class)
                .hasMessage("Turnstile verification failed");

        verify(abuseGuard).requireTurnstileForNextMessage(conversationId);
        verify(abuseSignalRecorder).record("support-chat", "turnstile_failed");
    }

    private static SupportChatProperties supportChatProperties() {
        return new SupportChatProperties(
                true,
                4000,
                90,
                SupportChatProperties.OwnerMessageMode.FAKE,
                null,
                null,
                new SupportChatProperties.RateLimits(
                        new SupportChatProperties.Bucket(20, Duration.ofMinutes(1)),
                        new SupportChatProperties.Bucket(100, Duration.ofHours(1)),
                        new SupportChatProperties.Bucket(300, Duration.ofDays(1)),
                        new SupportChatProperties.Bucket(10, Duration.ofSeconds(10)),
                        new SupportChatProperties.Bucket(60, Duration.ofMinutes(1))),
                Set.of());
    }

    private static SupportMessageEntity previousCustomerMessage(UUID conversationId, String duplicateKey) {
        var message = new SupportMessageEntity();
        message.setConversationId(conversationId);
        message.setNormalizedBody(duplicateKey);
        message.setDeliveryStatus(SENT);
        return message;
    }
}
