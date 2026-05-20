package com.zufar.icedlatte.payment.api.webhook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookEventRecorder unit tests")
class StripeWebhookEventRecorderTest {

    @Mock private StripeWebhookEventTransactionService txService;
    @InjectMocks private StripeWebhookEventRecorder recorder;

    @Test
    @DisplayName("tryAcquire delegates insert to txService and returns true")
    void tryAcquire_newEvent_returnsTrue() {
        when(txService.tryInsertNewEvent("evt_1", "checkout.session.completed")).thenReturn(true);

        assertThat(recorder.tryAcquire("evt_1", "checkout.session.completed")).isTrue();
        verify(txService).tryInsertNewEvent("evt_1", "checkout.session.completed");
    }

    @Test
    @DisplayName("tryAcquire falls back to re-acquire on duplicate")
    void tryAcquire_duplicate_fallsBackToReacquire() {
        when(txService.tryInsertNewEvent("evt_1", "checkout.session.completed"))
                .thenThrow(new DataIntegrityViolationException("dup"));
        when(txService.tryReacquireRetryableEvent("evt_1")).thenReturn(true);

        assertThat(recorder.tryAcquire("evt_1", "checkout.session.completed")).isTrue();
        verify(txService).tryReacquireRetryableEvent("evt_1");
    }

    @Test
    @DisplayName("tryAcquire returns false when duplicate is not retryable")
    void tryAcquire_duplicateNotRetryable_returnsFalse() {
        when(txService.tryInsertNewEvent("evt_1", "checkout.session.completed"))
                .thenThrow(new DataIntegrityViolationException("dup"));
        when(txService.tryReacquireRetryableEvent("evt_1")).thenReturn(false);

        assertThat(recorder.tryAcquire("evt_1", "checkout.session.completed")).isFalse();
    }

    @Test
    @DisplayName("markProcessed delegates to txService")
    void markProcessed_delegates() {
        recorder.markProcessed("evt_1");
        verify(txService).markProcessed("evt_1");
    }

    @Test
    @DisplayName("markRetryableFailed delegates to txService")
    void markRetryableFailed_delegates() {
        recorder.markRetryableFailed("evt_1", "timeout");
        verify(txService).markRetryableFailed("evt_1", "timeout");
    }
}
