package com.zufar.icedlatte.review.service.ai;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zufar.icedlatte.ratelimit.api.DailyOperationQuota;
import com.zufar.icedlatte.review.config.AiUsageProperties;

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AiUsageGuard {
    private final DailyOperationQuota quota;
    private final AiUsageProperties properties;
    private final Semaphore slots;

    public AiUsageGuard(DailyOperationQuota quota, AiUsageProperties properties) {
        this.quota = quota;
        this.properties = properties;
        this.slots = new Semaphore(properties.concurrentCalls());
    }

    public String call(String input, Supplier<String> operation) {
        if (input.length() > properties.maxInputChars()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI input is too long.");
        }
        if (!slots.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI is busy. Try again later.");
        }
        try {
            quota.reserve("ai:reviews", properties.dailyCalls());
            return operation.get();
        } finally {
            slots.release();
        }
    }
}
