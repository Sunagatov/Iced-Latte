package com.zufar.icedlatte.astartup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("StartupTaskRunner unit tests")
@SuppressWarnings("unused")
class StartupTaskRunnerTest {

    @Test
    @DisplayName("does not throw when a task finishes before timeout scheduling completes")
    void doesNotThrowWhenTaskFinishesImmediately() throws InterruptedException {
        int taskCount = 100;
        CountDownLatch completedTasks = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            assertThatCode(() -> StartupTaskRunner.runAsync(
                            "fast startup task", Duration.ofSeconds(5), completedTasks::countDown))
                    .doesNotThrowAnyException();
        }

        assertThat(completedTasks.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("interrupts cooperative tasks when they exceed the timeout")
    void interruptsTimedOutTasks() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        StartupTaskRunner.runAsync("slow startup task", Duration.ofMillis(10), () -> {
            started.countDown();
            try {
                awaitLatchUntilTimeout();
            } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        });

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("does not log timed out tasks as normal finishes")
    void doesNotLogTimedOutTasksAsNormalFinishes(CapturedOutput output) throws InterruptedException {
        CountDownLatch finished = new CountDownLatch(1);

        StartupTaskRunner.runAsync("interrupt-swallowing task", Duration.ofMillis(10), () -> {
            try {
                awaitLatchUntilTimeout();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } finally {
                finished.countDown();
            }
        });

        assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(waitForOutput(output, "startup.task.timeout: task=interrupt-swallowing task"))
                .isTrue();
        assertThat(waitForOutput(output, "startup.task.finish_after_timeout: task=interrupt-swallowing task"))
                .isTrue();
        assertThat(output).doesNotContain("startup.task.finish: task=interrupt-swallowing task");
    }

    private boolean waitForOutput(CapturedOutput output, String expectedMessage) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadlineNanos) {
            if (output.getOut().contains(expectedMessage)) {
                return true;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        return output.getOut().contains(expectedMessage);
    }

    private void awaitLatchUntilTimeout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isFalse();
    }
}
