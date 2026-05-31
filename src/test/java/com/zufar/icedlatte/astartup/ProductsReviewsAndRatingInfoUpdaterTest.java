package com.zufar.icedlatte.astartup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.api.ReviewMaintenanceApi;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductsReviewsAndRatingInfoUpdater unit tests")
class ProductsReviewsAndRatingInfoUpdaterTest {

    @Mock
    private ProductReviewProductApi productReviewProductGateway;

    @Mock
    private ReviewMaintenanceApi reviewMaintenanceApi;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationArguments args;

    private ProductsReviewsAndRatingInfoUpdater updater;

    @BeforeEach
    void setUp() {
        updater = updater(true, Duration.ofSeconds(5));
    }

    @Nested
    @DisplayName("run")
    class Run {

        @Test
        @DisplayName("skips when ratings migration is disabled")
        void skipsWhenDisabled() {
            updater = updater(false, Duration.ofSeconds(5));

            updater.run(args);

            verifyNoInteractions(transactionTemplate, productReviewProductGateway, reviewMaintenanceApi);
        }

        @Test
        @DisplayName("refreshes all rating and review metadata in order")
        void refreshesAllRatingAndReviewMetadataInOrder() {
            doAnswer(invocation -> {
                        Consumer<TransactionStatus> callback = invocation.getArgument(0);
                        callback.accept(null);
                        return null;
                    })
                    .when(transactionTemplate)
                    .executeWithoutResult(any());

            updater.run(args);

            verify(transactionTemplate, timeout(1000)).executeWithoutResult(any());
            InOrder inOrder = inOrder(productReviewProductGateway, reviewMaintenanceApi);
            inOrder.verify(productReviewProductGateway).refreshAllReviewAggregates();
            inOrder.verify(reviewMaintenanceApi).refreshAllCounts();
            verifyNoMoreInteractions(transactionTemplate, productReviewProductGateway, reviewMaintenanceApi);
        }

        @Test
        @DisplayName("does not propagate async transaction failures to the startup caller")
        void doesNotPropagateAsyncTransactionFailuresToStartupCaller() {
            doThrow(new IllegalStateException("boom")).when(transactionTemplate).executeWithoutResult(any());

            assertThatCode(() -> updater.run(args)).doesNotThrowAnyException();

            verify(transactionTemplate, timeout(1000)).executeWithoutResult(any());
            verifyNoInteractions(reviewMaintenanceApi);
            verifyNoMoreInteractions(transactionTemplate, productReviewProductGateway, reviewMaintenanceApi);
        }

        @Test
        @DisplayName("does not block startup when ratings migration times out")
        void doesNotBlockStartupWhenRatingsMigrationTimesOut() {
            updater = updater(true, Duration.ofMillis(10));
            doAnswer(_ -> {
                        Thread.sleep(Duration.ofSeconds(5));
                        return null;
                    })
                    .when(transactionTemplate)
                    .executeWithoutResult(any());

            assertThatCode(() -> updater.run(args)).doesNotThrowAnyException();

            verify(transactionTemplate, timeout(1000)).executeWithoutResult(any());
            verifyNoInteractions(productReviewProductGateway, reviewMaintenanceApi);
            verifyNoMoreInteractions(transactionTemplate);
        }
    }

    private ProductsReviewsAndRatingInfoUpdater updater(boolean ratingsEnabled, Duration timeout) {
        return new ProductsReviewsAndRatingInfoUpdater(
                productReviewProductGateway,
                reviewMaintenanceApi,
                transactionTemplate,
                new MigrationProperties(
                        new MigrationProperties.Upload(false),
                        new MigrationProperties.Ratings(ratingsEnabled),
                        timeout,
                        null));
    }
}
