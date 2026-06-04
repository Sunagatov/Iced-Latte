package com.zufar.icedlatte.astartup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("StartupTaskRunner unit tests")
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
                Thread.sleep(Duration.ofSeconds(5));
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
                Thread.sleep(Duration.ofSeconds(5));
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } finally {
                finished.countDown();
            }
        });

        assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(output).contains("startup.task.timeout: task=interrupt-swallowing task");
        assertThat(output).contains("startup.task.finish_after_timeout: task=interrupt-swallowing task");
        assertThat(output).doesNotContain("startup.task.finish: task=interrupt-swallowing task");
    }
}
