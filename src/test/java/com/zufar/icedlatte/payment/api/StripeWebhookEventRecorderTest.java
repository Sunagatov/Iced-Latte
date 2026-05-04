package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;
import com.zufar.icedlatte.payment.entity.WebhookEventStatus;
import com.zufar.icedlatte.payment.repository.StripeWebhookEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookEventRecorder unit tests")
class StripeWebhookEventRecorderTest {

    @Mock private StripeWebhookEventRepository repository;
    @InjectMocks private StripeWebhookEventRecorder recorder;

    @Test
    @DisplayName("tryAcquire inserts new event and returns true")
    void tryAcquire_newEvent_returnsTrue() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = recorder.tryAcquire("evt_123", "checkout.session.completed");

        assertThat(result).isTrue();
        ArgumentCaptor<StripeWebhookEvent> captor = ArgumentCaptor.forClass(StripeWebhookEvent.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStripeEventId()).isEqualTo("evt_123");
        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookEventStatus.PROCESSING);
    }

    @Test
    @DisplayName("tryAcquire returns false for duplicate PROCESSING event")
    void tryAcquire_duplicateProcessing_returnsFalse() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        StripeWebhookEvent existing = new StripeWebhookEvent(
                "evt_123", "checkout.session.completed", WebhookEventStatus.PROCESSING,
                OffsetDateTime.now(), null, null);
        when(repository.findById("evt_123")).thenReturn(Optional.of(existing));

        boolean result = recorder.tryAcquire("evt_123", "checkout.session.completed");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("tryAcquire returns false for duplicate PROCESSED event")
    void tryAcquire_duplicateProcessed_returnsFalse() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        StripeWebhookEvent existing = new StripeWebhookEvent(
                "evt_123", "checkout.session.completed", WebhookEventStatus.PROCESSED,
                OffsetDateTime.now(), OffsetDateTime.now(), null);
        when(repository.findById("evt_123")).thenReturn(Optional.of(existing));

        boolean result = recorder.tryAcquire("evt_123", "checkout.session.completed");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("tryAcquire re-acquires RETRYABLE_FAILED event")
    void tryAcquire_retryableFailed_reAcquires() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        StripeWebhookEvent existing = new StripeWebhookEvent(
                "evt_123", "checkout.session.completed", WebhookEventStatus.RETRYABLE_FAILED,
                OffsetDateTime.now(), null, "DB timeout");
        when(repository.findById("evt_123")).thenReturn(Optional.of(existing));

        boolean result = recorder.tryAcquire("evt_123", "checkout.session.completed");

        assertThat(result).isTrue();
        assertThat(existing.getStatus()).isEqualTo(WebhookEventStatus.PROCESSING);
        assertThat(existing.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("markProcessed sets PROCESSED status and processedAt")
    void markProcessed_setsStatus() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_123", "checkout.session.completed", WebhookEventStatus.PROCESSING,
                OffsetDateTime.now(), null, null);
        when(repository.findById("evt_123")).thenReturn(Optional.of(event));

        recorder.markProcessed("evt_123");

        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("markRetryableFailed sets RETRYABLE_FAILED status and reason")
    void markRetryableFailed_setsStatusAndReason() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_123", "checkout.session.completed", WebhookEventStatus.PROCESSING,
                OffsetDateTime.now(), null, null);
        when(repository.findById("evt_123")).thenReturn(Optional.of(event));

        recorder.markRetryableFailed("evt_123", "Connection timeout");

        assertThat(event.getStatus()).isEqualTo(WebhookEventStatus.RETRYABLE_FAILED);
        assertThat(event.getFailureReason()).isEqualTo("Connection timeout");
    }
}
