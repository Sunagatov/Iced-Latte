package com.zufar.icedlatte.common.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulingConfiguration {

    @Bean
    TaskScheduler taskScheduler(
            @Value("${app.scheduling.pool-size:4}") int poolSize,
            @Value("${app.scheduling.thread-name-prefix:iced-latte-scheduler-}") String threadNamePrefix,
            @Value("${app.scheduling.await-termination:PT30S}") String awaitTerminationValue) {
        if (poolSize < 1) {
            throw new IllegalStateException("app.scheduling.pool-size must be positive");
        }
        Duration awaitTermination = Duration.parse(awaitTerminationValue);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationMillis(awaitTermination.toMillis());
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
