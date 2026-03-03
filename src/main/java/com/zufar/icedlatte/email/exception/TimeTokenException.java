package com.zufar.icedlatte.email.exception;

import lombok.Getter;

import java.time.Duration;
import java.time.OffsetDateTime;

@Getter
public class TimeTokenException extends RuntimeException {

    private final String email;

    public TimeTokenException(String email, OffsetDateTime expireTime) {
        super(buildMessageError(email, expireTime));
        this.email = email;
    }

    private static String buildMessageError(String email, OffsetDateTime expireTime) {
        StringBuilder stringBuilder = new StringBuilder();
        Duration remainingTime = Duration.between(OffsetDateTime.now(), expireTime);
        long minutes = remainingTime.toMinutesPart();
        long seconds = remainingTime.toSecondsPart();

        stringBuilder.append("Token for email '").append(email).append("' will be expired after: ");
        if (minutes != 0) {
            stringBuilder.append(minutes).append(" min ");
        }
        stringBuilder.append(seconds).append(" sec");
        return stringBuilder.toString();
    }
}
