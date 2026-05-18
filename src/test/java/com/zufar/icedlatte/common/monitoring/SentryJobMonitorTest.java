package com.zufar.icedlatte.common.monitoring;

import io.sentry.MonitorScheduleUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SentryJobMonitor unit tests")
class SentryJobMonitorTest {

    @Test
    @DisplayName("runs job when Sentry is disabled")
    void runsJobWhenSentryDisabled() {
        SentryJobMonitor monitor = new SentryJobMonitor();
        ReflectionTestUtils.setField(monitor, "sentryEnabled", false);
        AtomicBoolean executed = new AtomicBoolean(false);

        monitor.run("test-job", monitor.fixedDelayConfig(60_000), () -> executed.set(true));

        assertThat(executed).isTrue();
    }

    @Test
    @DisplayName("propagates job failures when Sentry is disabled")
    void propagatesJobFailuresWhenSentryDisabled() {
        SentryJobMonitor monitor = new SentryJobMonitor();
        ReflectionTestUtils.setField(monitor, "sentryEnabled", false);
        RuntimeException exception = new RuntimeException("job failed");

        assertThatThrownBy(() -> monitor.run("test-job", monitor.fixedDelayConfig(60_000), () -> {
            throw exception;
        })).isSameAs(exception);
    }

    @Test
    @DisplayName("converts Spring six-field cron to Sentry five-field crontab")
    void convertsSpringCronToSentryCrontab() {
        SentryJobMonitor monitor = new SentryJobMonitor();

        assertThat(monitor.toSentryCrontab("0 0/5 * * * *")).isEqualTo("0/5 * * * *");
        assertThat(monitor.toSentryCrontab("*/10 * * * * *")).isEqualTo("*/10 * * * * *");
    }

    @Test
    @DisplayName("builds fixed-delay monitor config in minutes")
    void buildsFixedDelayMonitorConfig() {
        SentryJobMonitor monitor = new SentryJobMonitor();

        var config = monitor.fixedDelayConfig(3_600_000);

        assertThat(config.getSchedule().getValue()).isEqualTo("60");
        assertThat(config.getSchedule().getUnit()).isEqualTo(MonitorScheduleUnit.MINUTE.apiName());
        assertThat(config.getCheckinMargin()).isEqualTo(5L);
    }
}
