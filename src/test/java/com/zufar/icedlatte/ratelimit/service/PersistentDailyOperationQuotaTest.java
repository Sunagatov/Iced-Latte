package com.zufar.icedlatte.ratelimit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.zaxxer.hikari.HikariDataSource;
import com.zufar.icedlatte.ratelimit.api.DailyOperationQuota;

class PersistentDailyOperationQuotaTest {
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:13.11-bullseye"));
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static DailyOperationQuota quota;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T23:59:59Z"), ZoneOffset.UTC);

    @BeforeAll
    static void setup() throws Exception {
        POSTGRES.start();
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class);
        quota = context.getBean(DailyOperationQuota.class);
        String migration = Files.readString(
                Path.of("src/main/resources/db/changelog/version-2.0/04.11.2026.part1.add-operation-quotas.sql"));
        jdbc.execute(migration.substring(migration.indexOf("CREATE TABLE"), migration.indexOf("-- rollback")));
    }

    @AfterAll
    static void close() {
        if (context != null) context.close();
        POSTGRES.stop();
    }

    @BeforeEach
    void clearUsage() {
        jdbc.update("DELETE FROM daily_operation_quota");
    }

    @Test
    void concurrentRequestsAcrossTwoInstancesCannotExceedTheSharedAllowance() throws Exception {
        var other = new PersistentDailyOperationQuota(jdbc, CLOCK);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = IntStream.range(0, 80)
                    .mapToObj(i -> (Callable<Boolean>) () -> {
                        try {
                            (i % 2 == 0 ? quota : other).reserve("avatar:global", 25);
                            return true;
                        } catch (ResponseStatusException ex) {
                            assertThat(ex.getStatusCode().value()).isEqualTo(429);
                            return false;
                        }
                    })
                    .toList();
            int allowed = 0;
            for (var result : executor.invokeAll(tasks)) if (result.get()) allowed++;
            assertThat(allowed).isEqualTo(25);
            assertThat(jdbc.queryForObject("SELECT used FROM daily_operation_quota", Long.class))
                    .isEqualTo(25);
        }
    }

    @Test
    void reservationsSurviveOuterTransactionRollbackAndServiceRecreation() {
        new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status -> {
            quota.reserve("ai:reviews", 1);
            status.setRollbackOnly();
            return null;
        });
        assertThatThrownBy(() -> new PersistentDailyOperationQuota(jdbc, CLOCK).reserve("ai:reviews", 1))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resetsAtUtcMidnightWithoutAddingDailyRowsOrResettingAfterClockMovesBackwards() {
        quota.reserve("ai:reviews", 1);
        var nextDay = new PersistentDailyOperationQuota(jdbc, Clock.offset(CLOCK, java.time.Duration.ofSeconds(1)));
        nextDay.reserve("ai:reviews", 1);
        assertThatThrownBy(() -> quota.reserve("ai:reviews", 1)).isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM daily_operation_quota", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void quotaReservationStillWorksWhenEveryBusinessPoolConnectionIsHeld() throws Exception {
        var held = new java.util.ArrayList<java.sql.Connection>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var main = context.getBean(HikariDataSource.class);
            for (int i = 0; i < 5; i++) held.add(main.getConnection());
            worker.submit(() -> quota.reserve("avatar:global", 100)).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            for (var connection : held) connection.close();
        }
        assertThat(jdbc.queryForObject("SELECT used FROM daily_operation_quota", Long.class))
                .isEqualTo(1);
    }

    @Test
    void unavailableDatabaseFailsClosed() {
        var failed = mock(JdbcTemplate.class);
        when(failed.queryForList(anyString(), eq(Long.class), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        assertThatThrownBy(() -> new PersistentDailyOperationQuota(failed, CLOCK).reserve("ai:reviews", 100))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean
        HikariDataSource dataSource() {
            var source = new HikariDataSource();
            source.setJdbcUrl(POSTGRES.getJdbcUrl());
            source.setUsername(POSTGRES.getUsername());
            source.setPassword(POSTGRES.getPassword());
            source.setMaximumPoolSize(5);
            return source;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        DailyOperationQuota quota(HikariDataSource main) {
            return new PersistentDailyOperationQuota(main, CLOCK);
        }
    }
}
