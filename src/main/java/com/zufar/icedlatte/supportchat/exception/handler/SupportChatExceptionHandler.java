package com.zufar.icedlatte.supportchat.exception.handler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.turnstile.TurnstileVerificationException;
import com.zufar.icedlatte.supportchat.exception.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SupportChatExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(SupportChatException.class)
    public ResponseEntity<ProblemDetail> handleSupportChatException(final SupportChatException ex) {
        var mapping =
                switch (ex) {
                    case SupportChatDisabledException _ ->
                        new ErrorMapping(
                                "exception.support_chat.disabled",
                                ProblemType.SUPPORT_CHAT_DISABLED,
                                "Support chat unavailable",
                                HttpStatus.NOT_FOUND,
                                ex.getMessage());
                    case SupportChatEmailVerificationRequiredException _ ->
                        new ErrorMapping(
                                "exception.support_chat.email_verification_required",
                                ProblemType.SUPPORT_CHAT_EMAIL_VERIFICATION_REQUIRED,
                                "Email verification required",
                                HttpStatus.FORBIDDEN,
                                ex.getMessage());
                    case SupportChatAccessRestrictedException _ ->
                        new ErrorMapping(
                                "exception.support_chat.access_restricted",
                                ProblemType.SUPPORT_CHAT_ACCESS_RESTRICTED,
                                "Support chat unavailable",
                                HttpStatus.FORBIDDEN,
                                ex.getMessage());
                    case SupportChatConversationNotFoundException _ ->
                        new ErrorMapping(
                                "exception.support_chat.conversation_not_found",
                                ProblemType.SUPPORT_CHAT_CONVERSATION_NOT_FOUND,
                                "Support conversation not found",
                                HttpStatus.NOT_FOUND,
                                ex.getMessage());
                    case InvalidSupportChatMessageException _ ->
                        new ErrorMapping(
                                "exception.support_chat.invalid_message",
                                ProblemType.SUPPORT_CHAT_INVALID_MESSAGE,
                                "Invalid support chat message",
                                HttpStatus.BAD_REQUEST,
                                ex.getMessage());
                    case DuplicateSupportChatMessageException _ ->
                        new ErrorMapping(
                                "exception.support_chat.duplicate_message",
                                ProblemType.SUPPORT_CHAT_DUPLICATE_MESSAGE,
                                "Repeated message rejected",
                                HttpStatus.CONFLICT,
                                ex.getMessage());
                    case SupportChatRateLimitExceededException _ ->
                        new ErrorMapping(
                                "exception.support_chat.rate_limited",
                                ProblemType.SUPPORT_CHAT_RATE_LIMITED,
                                "Too many support chat messages",
                                HttpStatus.TOO_MANY_REQUESTS,
                                ex.getMessage());
                    case SupportChatOwnerDeliveryFailedException _ ->
                        new ErrorMapping(
                                "exception.support_chat.owner_delivery_failed",
                                ProblemType.SUPPORT_CHAT_TEMPORARILY_UNAVAILABLE,
                                "Support chat temporarily unavailable",
                                HttpStatus.SERVICE_UNAVAILABLE,
                                ex.getMessage());
                };

        HttpStatus status = mapping.status();
        log.debug("{}: status={}", mapping.logTag(), status.value());
        return ResponseEntity.status(status)
                .body(problemDetailFactory.build(mapping.typeSlug(), mapping.title(), status, mapping.detail()));
    }

    @ExceptionHandler(TurnstileVerificationException.class)
    public ResponseEntity<ProblemDetail> handleTurnstileVerificationException(final TurnstileVerificationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.debug("exception.support_chat.turnstile_failed: status={}", status.value());
        return ResponseEntity.status(status)
                .body(problemDetailFactory.build(
                        ProblemType.SUPPORT_CHAT_TURNSTILE_FAILED,
                        "Support chat verification failed",
                        status,
                        "Please retry the verification challenge."));
    }

    private record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}
}
