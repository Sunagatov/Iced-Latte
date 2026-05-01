package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewSummaryDebouncer unit tests")
class ProductReviewSummaryDebouncerTest {

    @Mock private ProductSummaryService productSummaryService;
    @Mock private ProductReviewProductGateway productReviewProductGateway;
    @Mock private ApplicationContext applicationContext;
    @Mock private ScheduledExecutorService scheduler;
    @Mock private ScheduledFuture<Object> future;
    @Mock private ScheduledFuture<Object> existingFuture;

    @InjectMocks private ProductReviewSummaryDebouncer debouncer;

    @AfterEach
    void shutdown() {
        debouncer.shutdown();
    }

    @Test
    @DisplayName("schedule replaces an existing debounce task for the same product")
    void scheduleReplacesExistingDebounceTaskForSameProduct() {
        UUID productId = UUID.randomUUID();
        ReflectionTestUtils.setField(debouncer, "scheduler", scheduler);
        pending().put(productId, existingFuture);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), eq(120L), eq(TimeUnit.SECONDS));

        debouncer.schedule(productId);

        verify(existingFuture).cancel(false);
        verify(scheduler).schedule(any(Runnable.class), eq(120L), eq(TimeUnit.SECONDS));
        assertThat(pending()).containsEntry(productId, future);
        assertThat(firstTriggerTime()).containsKey(productId);
    }

    @Test
    @DisplayName("schedule executes immediately after the max wait window")
    void scheduleExecutesImmediatelyAfterMaxWaitWindow() {
        UUID productId = UUID.randomUUID();
        ReflectionTestUtils.setField(debouncer, "scheduler", scheduler);
        firstTriggerTime().put(productId, System.currentTimeMillis() - 601_000L);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));

        debouncer.schedule(productId);

        verify(scheduler).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("scheduled runnable calls the proxied bean summary method")
    void scheduledRunnableCallsProxiedBeanSummaryMethod() {
        UUID productId = UUID.randomUUID();
        ReflectionTestUtils.setField(debouncer, "scheduler", scheduler);
        when(applicationContext.getBean(ProductReviewSummaryDebouncer.class)).thenReturn(debouncer);
        when(scheduler.schedule(any(Runnable.class), any(Long.class), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return future;
                });

        debouncer.schedule(productId);

        verify(applicationContext).getBean(ProductReviewSummaryDebouncer.class);
    }

    @Test
    @DisplayName("runSummary saves the generated summary and clears pending state")
    void runSummarySavesGeneratedSummaryAndClearsPendingState() {
        UUID productId = UUID.randomUUID();
        when(productSummaryService.summarize(productId)).thenReturn("Fresh summary");
        pending().put(productId, future);
        firstTriggerTime().put(productId, 123L);

        debouncer.runSummary(productId);

        verify(productReviewProductGateway).updateAiSummary(productId, "Fresh summary");
        assertThat(pending()).doesNotContainKey(productId);
        assertThat(firstTriggerTime()).doesNotContainKey(productId);
    }

    @Test
    @DisplayName("runSummary delegates summary persistence through the product gateway")
    void runSummaryDelegatesSummaryPersistenceThroughProductGateway() {
        UUID productId = UUID.randomUUID();
        when(productSummaryService.summarize(productId)).thenReturn("Fresh summary");

        debouncer.runSummary(productId);

        verify(productReviewProductGateway).updateAiSummary(productId, "Fresh summary");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<UUID, ScheduledFuture<?>> pending() {
        return (ConcurrentHashMap<UUID, ScheduledFuture<?>>) ReflectionTestUtils.getField(debouncer, "pendingDebounce");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<UUID, Long> firstTriggerTime() {
        return (ConcurrentHashMap<UUID, Long>) ReflectionTestUtils.getField(debouncer, "firstTriggerTime");
    }
}
