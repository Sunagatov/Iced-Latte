package com.zufar.icedlatte.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("TaskSchedulingConfiguration context tests")
class TaskSchedulingConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TaskSchedulingConfiguration.class);

    @Test
    @DisplayName("creates a thread pool scheduler with configured values")
    void createsThreadPoolSchedulerWithConfiguredValues() {
        contextRunner
                .withPropertyValues(
                        "app.scheduling.pool-size=6",
                        "app.scheduling.thread-name-prefix=ops-scheduler-",
                        "app.scheduling.await-termination=PT45S")
                .run(context -> {
                    assertThat(context).hasSingleBean(ThreadPoolTaskScheduler.class);
                    ThreadPoolTaskScheduler scheduler = context.getBean(ThreadPoolTaskScheduler.class);
                    assertThat(ReflectionTestUtils.getField(scheduler, "poolSize"))
                            .isEqualTo(6);
                    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("ops-scheduler-");
                    assertThat(ReflectionTestUtils.getField(scheduler, "awaitTerminationMillis"))
                            .isEqualTo(45_000L);
                    assertThat(ReflectionTestUtils.getField(scheduler, "waitForTasksToCompleteOnShutdown"))
                            .isEqualTo(true);
                });
    }
}
