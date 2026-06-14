package com.zufar.icedlatte.review.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.review.exception.ReviewAccessDeniedException;
import com.zufar.icedlatte.review.exception.ReviewConflictException;
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.exception.ReviewNotFoundException;
import com.zufar.icedlatte.review.exception.ReviewProductNotFoundException;
import com.zufar.icedlatte.review.exception.ReviewSummaryException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewExceptionHandler unit tests")
class ReviewExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private ReviewExceptionHandler reviewExceptionHandler;

    @Test
    @DisplayName("returns 403 for review access denied")
    void returns403ForReviewAccessDenied() {
        ReviewAccessDeniedException exception = new ReviewAccessDeniedException();
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        when(problemDetailFactory.build(
                        ProblemType.REVIEW_ACCESS_DENIED, "Access denied", HttpStatus.FORBIDDEN, "Access denied."))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = reviewExceptionHandler.handleReviewException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns 409 for review conflicts")
    void returns409ForReviewConflicts() {
        ReviewConflictException exception = new ReviewConflictException("conflict");
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        when(problemDetailFactory.build(
                        ProblemType.REVIEW_CONFLICT, "Review conflict", HttpStatus.CONFLICT, exception.getMessage()))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = reviewExceptionHandler.handleReviewException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns 422 for rejected reviews")
    void returns422ForRejectedReviews() {
        ReviewModerationException exception = new ReviewModerationException("spam detected");
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        when(problemDetailFactory.build(
                        ProblemType.REVIEW_REJECTED,
                        "Review rejected",
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        exception.getMessage()))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = reviewExceptionHandler.handleReviewException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns 404 for missing reviews")
    void returns404ForMissingReviews() {
        ReviewNotFoundException exception = new ReviewNotFoundException(UUID.randomUUID());
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        when(problemDetailFactory.build(
                        ProblemType.REVIEW_NOT_FOUND, "Review not found", HttpStatus.NOT_FOUND, exception.getMessage()))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = reviewExceptionHandler.handleReviewException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns 404 for missing products in review flows")
    void returns404ForMissingProductsInReviewFlows() {
        ReviewProductNotFoundException exception = new ReviewProductNotFoundException(UUID.randomUUID());
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        when(problemDetailFactory.build(
                        ProblemType.PRODUCT_NOT_FOUND,
                        "Product not found",
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = reviewExceptionHandler.handleReviewException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("returns 503 for summary failures")
    void returns503ForSummaryFailures() {
        ReviewSummaryException exception =
                new ReviewSummaryException(UUID.randomUUID(), new RuntimeException("timeout"));
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        when(problemDetailFactory.build(
                        ProblemType.REVIEW_SUMMARY_UNAVAILABLE,
                        "Review summary unavailable",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Review summary is currently unavailable."))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = reviewExceptionHandler.handleReviewException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getBody()).isEqualTo(expected);
    }
}
