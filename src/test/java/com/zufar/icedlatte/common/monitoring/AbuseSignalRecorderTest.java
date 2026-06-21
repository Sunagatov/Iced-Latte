package com.zufar.icedlatte.common.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@DisplayName("AbuseSignalRecorder unit tests")
class AbuseSignalRecorderTest {

    @Test
    @DisplayName("records abuse signal counters with surface and signal tags")
    void record_incrementsTaggedCounter() {
        var meterRegistry = new SimpleMeterRegistry();
        var recorder = new AbuseSignalRecorder(meterRegistry);

        recorder.record("checkout", "idempotency_collision");
        recorder.record("checkout", "idempotency_collision");

        assertThat(meterRegistry
                        .get("security.abuse.signals")
                        .tag("surface", "checkout")
                        .tag("signal", "idempotency_collision")
                        .counter()
                        .count())
                .isEqualTo(2.0);
    }
}
