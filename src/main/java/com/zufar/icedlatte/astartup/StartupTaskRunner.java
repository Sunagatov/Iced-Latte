package com.zufar.icedlatte.astartup;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class StartupTaskRunner {

    private StartupTaskRunner() {}

    static void runAsync(String taskName, Duration timeout, StartupTask task) {
        Thread.startVirtualThread(() -> runWithTimeout(taskName, timeout, task));
    }

    private static void runWithTimeout(String taskName, Duration timeout, StartupTask task) {
        try (var timeoutScheduler = Executors.newSingleThreadScheduledExecutor()) {
            var taskThread = Thread.currentThread();
            var taskFinished = new AtomicBoolean(false);
            var taskTimedOut = new AtomicBoolean(false);
            var timeoutFuture = timeoutScheduler.schedule(
                    () -> {
                        if (!taskFinished.get()) {
                            taskTimedOut.set(true);
                            log.error("startup.task.timeout: task={}, timeout={}", taskName, timeout);
                            taskThread.interrupt();
                            timeoutScheduler.shutdownNow();
                        }
                    },
                    Math.max(1, timeout.toMillis()),
                    TimeUnit.MILLISECONDS);

            try {
                log.info("startup.task.start: task={}", taskName);
                task.run();
                if (taskTimedOut.get()) {
                    log.warn("startup.task.finish_after_timeout: task={}", taskName);
                } else {
                    log.info("startup.task.finish: task={}", taskName);
                }
            } catch (Exception e) {
                log.error(
                        "startup.task.error: task={}, exceptionClass={}",
                        taskName,
                        e.getClass().getSimpleName(),
                        e);
            } finally {
                taskFinished.set(true);
                timeoutFuture.cancel(false);
                timeoutScheduler.shutdownNow();
            }
        }
    }

    @FunctionalInterface
    interface StartupTask {

        void run();
    }
}
