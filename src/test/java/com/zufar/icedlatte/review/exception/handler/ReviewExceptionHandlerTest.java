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
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.exception.ReviewSummaryException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewExceptionHandler unit tests")
class ReviewExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private ReviewExceptionHandler reviewExceptionHandler;

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
