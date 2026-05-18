package com.zufar.icedlatte.common.monitoring;

import io.sentry.CheckIn;
import io.sentry.CheckInStatus;
import io.sentry.MonitorConfig;
import io.sentry.MonitorSchedule;
import io.sentry.MonitorScheduleUnit;
import io.sentry.Sentry;
import io.sentry.protocol.SentryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SentryJobMonitor {

    @Value("${sentry.enabled:false}")
    private boolean sentryEnabled;

    public void run(String monitorSlug, MonitorConfig monitorConfig, Runnable job) {
        SentryId checkInId = captureCheckIn(monitorSlug, monitorConfig);
        long startedAt = System.nanoTime();
        try {
            job.run();
            captureCheckIn(monitorSlug, CheckInStatus.OK, monitorConfigForFinalCheckIn(monitorConfig, checkInId),
                    durationSeconds(startedAt), checkInId);
        } catch (RuntimeException | Error e) {
            captureCheckIn(monitorSlug, CheckInStatus.ERROR, monitorConfigForFinalCheckIn(monitorConfig, checkInId),
                    durationSeconds(startedAt), checkInId);
            throw e;
        }
    }

    public MonitorConfig cronConfig(String springCron) {
        MonitorConfig monitorConfig = new MonitorConfig(MonitorSchedule.crontab(toSentryCrontab(springCron)));
        monitorConfig.setCheckinMargin(2L);
        monitorConfig.setMaxRuntime(2L);
        monitorConfig.setFailureIssueThreshold(1L);
        monitorConfig.setRecoveryThreshold(1L);
        monitorConfig.setTimezone("UTC");
        return monitorConfig;
    }

    public MonitorConfig fixedDelayConfig(long fixedDelayMs) {
        long intervalMinutes = Math.max(1, TimeUnit.MILLISECONDS.toMinutes(fixedDelayMs));
        MonitorConfig monitorConfig = new MonitorConfig(MonitorSchedule.interval(Math.toIntExact(intervalMinutes),
                MonitorScheduleUnit.MINUTE));
        monitorConfig.setCheckinMargin(5L);
        monitorConfig.setMaxRuntime(Math.max(5L, intervalMinutes));
        monitorConfig.setFailureIssueThreshold(1L);
        monitorConfig.setRecoveryThreshold(1L);
        return monitorConfig;
    }

    String toSentryCrontab(String springCron) {
        if (!StringUtils.hasText(springCron)) {
            return springCron;
        }
        String[] fields = springCron.trim().split("\\s+");
        if (fields.length == 6 && "0".equals(fields[0])) {
            return String.join(" ", fields[1], fields[2], fields[3], fields[4], fields[5]);
        }
        return springCron;
    }

    private SentryId captureCheckIn(String monitorSlug,
                                    MonitorConfig monitorConfig) {
        return captureCheckIn(monitorSlug, CheckInStatus.IN_PROGRESS, monitorConfig, null, null);
    }

    private SentryId captureCheckIn(String monitorSlug,
                                    CheckInStatus status,
                                    MonitorConfig monitorConfig,
                                    Double duration,
                                    SentryId checkInId) {
        if (!sentryEnabled || !Sentry.isEnabled()) {
            return SentryId.EMPTY_ID;
        }
        try {
            CheckIn checkIn = newCheckIn(monitorSlug, status, checkInId);
            checkIn.setMonitorConfig(monitorConfig);
            checkIn.setDuration(duration);
            return Sentry.captureCheckIn(checkIn);
        } catch (RuntimeException e) {
            log.debug("sentry.monitor.check_in_failed: monitorSlug={}, status={}, exceptionClass={}",
                    monitorSlug, status.apiName(), e.getClass().getSimpleName());
            return SentryId.EMPTY_ID;
        }
    }

    private CheckIn newCheckIn(String monitorSlug, CheckInStatus status, SentryId checkInId) {
        if (checkInId == null || SentryId.EMPTY_ID.equals(checkInId)) {
            return new CheckIn(monitorSlug, status);
        }
        return new CheckIn(checkInId, monitorSlug, status);
    }

    private MonitorConfig monitorConfigForFinalCheckIn(MonitorConfig monitorConfig, SentryId checkInId) {
        return SentryId.EMPTY_ID.equals(checkInId) ? monitorConfig : null;
    }

    private double durationSeconds(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000_000.0;
    }
}
