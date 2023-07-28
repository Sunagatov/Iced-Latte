package com.zufar.onlinestore.payment.exception;

import com.stripe.model.Event;
import lombok.Getter;

@Getter
public class UnsupportedScenarioExecutorException extends RuntimeException {

    private final Event event;

    public UnsupportedScenarioExecutorException(final Event event) {
        super(String.format("Scenario executor for event: %s is unsupported.", event));
        this.event = event;
    }
}
