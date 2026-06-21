package com.zufar.icedlatte.common.monitoring;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AbuseSignalRecorder {

    private final MeterRegistry meterRegistry;

    public void record(String surface, String signal) {
        meterRegistry
                .counter("security.abuse.signals", "surface", surface, "signal", signal)
                .increment();
    }
}
