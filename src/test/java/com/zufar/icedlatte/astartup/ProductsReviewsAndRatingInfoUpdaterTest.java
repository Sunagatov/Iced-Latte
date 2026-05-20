package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.api.ReviewMaintenanceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductsReviewsAndRatingInfoUpdater unit tests")
class ProductsReviewsAndRatingInfoUpdaterTest {

    @Mock private ProductReviewProductGateway productReviewProductGateway;
    @Mock private ReviewMaintenanceApi reviewMaintenanceApi;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private ApplicationArguments args;

    private ProductsReviewsAndRatingInfoUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new ProductsReviewsAndRatingInfoUpdater(
                productReviewProductGateway, reviewMaintenanceApi, transactionTemplate);
        ReflectionTestUtils.setField(updater, "enabled", true);
        ReflectionTestUtils.setField(updater, "timeoutMinutes", 5);
    }

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("skips when ratings migration is disabled")
        void skipsWhenDisabled() {
            ReflectionTestUtils.setField(updater, "enabled", false);

            updater.run(args);

            verifyNoInteractions(transactionTemplate, productReviewProductGateway, reviewMaintenanceApi);
        }

        @Test
        @DisplayName("executes all rating backfill updates inside the transaction callback")
        void executesAllUpdatesInTransaction() {
            doAnswer(invocation -> {
                Consumer<TransactionStatus> callback = invocation.getArgument(0);
                callback.accept(null);
                return null;
            }).when(transactionTemplate).executeWithoutResult(any());

            updater.run(args);

            verify(transactionTemplate, timeout(1000)).executeWithoutResult(any());
            InOrder inOrder = inOrder(productReviewProductGateway, reviewMaintenanceApi);
            inOrder.verify(productReviewProductGateway).refreshAllReviewAggregates();
            inOrder.verify(reviewMaintenanceApi).refreshAllCounts();
            verifyNoMoreInteractions(transactionTemplate, productReviewProductGateway, reviewMaintenanceApi);
        }

        @Test
        @DisplayName("does not propagate async transaction failures to the caller")
        void doesNotPropagateAsyncFailures() {
            doThrow(new IllegalStateException("boom"))
                    .when(transactionTemplate).executeWithoutResult(any());

            assertThatCode(() -> updater.run(args)).doesNotThrowAnyException();

            verify(transactionTemplate, timeout(1000)).executeWithoutResult(any());
            verifyNoInteractions(productReviewProductGateway, reviewMaintenanceApi);
            verifyNoMoreInteractions(transactionTemplate);
        }
    }
}
