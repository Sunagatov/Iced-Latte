package com.zufar.icedlatte.payment.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;
import com.zufar.icedlatte.payment.entity.WebhookEventStatus;
import com.zufar.icedlatte.payment.repository.StripeWebhookEventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookEventTransactionService unit tests")
class StripeWebhookEventTransactionServiceTest {

    @Mock
    private StripeWebhookEventRepository repository;

    @InjectMocks
    private StripeWebhookEventTransactionService service;

    @Test
    @DisplayName("tryInsertNewEvent saves event with PROCESSING status")
    void tryInsertNewEvent_savesProcessing() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.tryInsertNewEvent("evt_1", "checkout.session.completed"))
                .isTrue();

        ArgumentCaptor<StripeWebhookEvent> captor = ArgumentCaptor.forClass(StripeWebhookEvent.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStripeEventId()).isEqualTo("evt_1");
        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookEventStatus.PROCESSING);
    }

    @Test
    @DisplayName("tryReacquireRetryableEvent re-acquires RETRYABLE_FAILED event")
    void tryReacquireRetryableEvent_reAcquires() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1",
                "checkout.session.completed",
                WebhookEventStatus.RETRYABLE_FAILED,
                OffsetDateTime.now(),
                null,
                "DB timeout");
        when(repository.findById("evt_1")).thenReturn(Optional.of(event));

        assertThat(service.tryReacquireRetryableEvent("evt_1")).isTrue();
        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.PROCESSING);
        assertThat(event.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("tryReacquireRetryableEvent returns false for PROCESSED event")
    void tryReacquireRetryableEvent_processedEvent_returnsFalse() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1",
                "checkout.session.completed",
                WebhookEventStatus.PROCESSED,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null);
        when(repository.findById("evt_1")).thenReturn(Optional.of(event));

        assertThat(service.tryReacquireRetryableEvent("evt_1")).isFalse();
    }

    @Test
    @DisplayName("tryReacquireRetryableEvent returns false for fresh PROCESSING event")
    void tryReacquireRetryableEvent_freshProcessingEvent_returnsFalse() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1", "checkout.session.completed", WebhookEventStatus.PROCESSING, OffsetDateTime.now(), null, null);
        when(repository.findById("evt_1")).thenReturn(Optional.of(event));

        assertThat(service.tryReacquireRetryableEvent("evt_1")).isFalse();
    }

    @Test
    @DisplayName("tryReacquireRetryableEvent re-acquires stale PROCESSING event")
    void tryReacquireRetryableEvent_staleProcessingEvent_reAcquires() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1",
                "checkout.session.completed",
                WebhookEventStatus.PROCESSING,
                OffsetDateTime.now().minus(Duration.ofMinutes(6)),
                null,
                null);
        when(repository.findById("evt_1")).thenReturn(Optional.of(event));

        assertThat(service.tryReacquireRetryableEvent("evt_1")).isTrue();
        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.PROCESSING);
    }

    @Test
    @DisplayName("markProcessed sets PROCESSED status and processedAt")
    void markProcessed_setsStatus() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1", "checkout.session.completed", WebhookEventStatus.PROCESSING, OffsetDateTime.now(), null, null);
        when(repository.findById("evt_1")).thenReturn(Optional.of(event));

        service.markProcessed("evt_1");

        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("markRetryableFailed sets status and truncates long reason")
    void markRetryableFailed_setsStatusAndReason() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1", "checkout.session.completed", WebhookEventStatus.PROCESSING, OffsetDateTime.now(), null, null);
        when(repository.findById("evt_1")).thenReturn(Optional.of(event));

        service.markRetryableFailed("evt_1", "Connection timeout");

        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.RETRYABLE_FAILED);
        assertThat(event.getFailureReason()).isEqualTo("Connection timeout");
    }
}
