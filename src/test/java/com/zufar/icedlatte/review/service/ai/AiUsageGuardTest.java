package com.zufar.icedlatte.review.service.ai;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.zufar.icedlatte.ratelimit.api.DailyOperationQuota;
import com.zufar.icedlatte.review.config.AiUsageProperties;

class AiUsageGuardTest {
    private final DailyOperationQuota quota = mock(DailyOperationQuota.class);
    private final AiUsageGuard guard = new AiUsageGuard(quota, new AiUsageProperties(100, 1, 10, 256));

    @Test
    void refusesExternalCallWhenAllowanceCannotBeReservedAndReleasesSlot() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .doNothing()
                .when(quota)
                .reserve("ai:reviews", 100);
        var operation = mock(java.util.function.Supplier.class);
        assertThatThrownBy(() -> guard.call("hello", operation)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(operation);
        assertThat(guard.call("hello", () -> "OK")).isEqualTo("OK");
    }

    @Test
    void rejectsLargeInputBeforeReservingOrCallingProvider() {
        assertThatThrownBy(() -> guard.call("x".repeat(11), () -> fail("must not call AI")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(quota);
    }

    @Test
    void boundsConcurrentCallsWithoutQueuingOrConsumingAnotherAllowance() throws Exception {
        var started = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> guard.call("first", () -> {
                started.countDown();
                try {
                    assertThat(finish.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                }
                return "OK";
            }));
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> guard.call("second", () -> fail("must not call AI")))
                        .isInstanceOf(ResponseStatusException.class);
                verify(quota, times(1)).reserve("ai:reviews", 100);
            } finally {
                finish.countDown();
            }
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("OK");
        }
    }

    @Test
    void providerFailureReleasesSlotButDoesNotRefundPotentiallyBillableAttempt() {
        assertThatThrownBy(() -> guard.call("hello", () -> {
                    throw new IllegalStateException("timeout");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(guard.call("again", () -> "OK")).isEqualTo("OK");
        verify(quota, times(2)).reserve("ai:reviews", 100);
    }
}
