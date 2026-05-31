package com.zufar.icedlatte.security.signup.exception;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_EARLY)
public class TimeTokenException extends RuntimeException {

    public TimeTokenException(OffsetDateTime expireTime) {
        super(buildMessageError(expireTime));
    }

    private static String buildMessageError(OffsetDateTime expireTime) {
        StringBuilder stringBuilder = new StringBuilder();
        Duration remainingTime = Duration.between(OffsetDateTime.now(), expireTime);
        long minutes = remainingTime.toMinutesPart();
        long seconds = remainingTime.toSecondsPart();

        stringBuilder.append("Token will be expired after: ");
        if (minutes != 0) {
            stringBuilder.append(minutes).append(" min ");
        }
        stringBuilder.append(seconds).append(" sec");
        return stringBuilder.toString();
    }
}
