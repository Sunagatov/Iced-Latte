package com.zufar.icedlatte.review.exception.handler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.review.exception.ReviewException;
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.exception.ReviewSummaryException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ReviewExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(ReviewException.class)
    public ResponseEntity<ProblemDetail> handleReviewException(final ReviewException ex) {
        return switch (ex) {
            case ReviewModerationException _ ->
                problem(
                        "exception.review.rejected",
                        ProblemType.REVIEW_REJECTED,
                        "Review rejected",
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        ex.getMessage());
            case ReviewSummaryException _ ->
                problem(
                        "exception.review.summary_unavailable",
                        ProblemType.REVIEW_SUMMARY_UNAVAILABLE,
                        "Review summary unavailable",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Review summary is currently unavailable.");
        };
    }

    private ResponseEntity<ProblemDetail> problem(
            String logTag, String typeSlug, String title, HttpStatus status, String detail) {
        log.debug("{}: status={}", logTag, status.value());
        return ResponseEntity.status(status).body(problemDetailFactory.build(typeSlug, title, status, detail));
    }
}
