package com.zufar.icedlatte.payment.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookEventRecorder unit tests")
class StripeWebhookEventRecorderTest {

    @Mock
    private StripeWebhookEventTransactionService txService;

    @InjectMocks
    private StripeWebhookEventRecorder recorder;

    @Test
    @DisplayName("tryAcquire delegates insert to txService and returns true")
    void tryAcquire_newEvent_returnsTrue() {
        assertThat(recorder.tryAcquire("evt_1", "checkout.session.completed")).isTrue();
        verify(txService).insertNewEvent("evt_1", "checkout.session.completed");
        verifyNoMoreInteractions(txService);
    }

    @Test
    @DisplayName("tryAcquire falls back to re-acquire on duplicate")
    void tryAcquire_duplicate_fallsBackToReacquire() {
        doThrow(new DataIntegrityViolationException("dup"))
                .when(txService)
                .insertNewEvent("evt_1", "checkout.session.completed");
        when(txService.tryReacquireRetryableEvent("evt_1")).thenReturn(true);

        assertThat(recorder.tryAcquire("evt_1", "checkout.session.completed")).isTrue();
        verify(txService).tryReacquireRetryableEvent("evt_1");
    }

    @Test
    @DisplayName("tryAcquire returns false when duplicate is not retryable")
    void tryAcquire_duplicateNotRetryable_returnsFalse() {
        doThrow(new DataIntegrityViolationException("dup"))
                .when(txService)
                .insertNewEvent("evt_1", "checkout.session.completed");
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
