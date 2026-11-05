package com.zufar.icedlatte.ratelimit.service;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zufar.icedlatte.ratelimit.api.DailyOperationQuota;

@Service
public class PersistentDailyOperationQuota implements DailyOperationQuota {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final HikariDataSource quotaPool;

    @Autowired
    public PersistentDailyOperationQuota(HikariDataSource mainPool) {
        this(mainPool, Clock.systemUTC());
    }

    PersistentDailyOperationQuota(HikariDataSource mainPool, Clock clock) {
        // Reservations must commit independently, even while all main-pool connections
        // are held by requests waiting on user locks. Bound this pool to two connections.
        var config = new HikariConfig();
        config.setJdbcUrl(mainPool.getJdbcUrl());
        config.setUsername(mainPool.getUsername());
        config.setPassword(mainPool.getPassword());
        config.setDataSourceProperties(mainPool.getDataSourceProperties());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setInitializationFailTimeout(-1);
        config.setPoolName("operation-quota");
        this.quotaPool = new HikariDataSource(config);
        this.jdbc = new JdbcTemplate(quotaPool);
        this.clock = clock;
    }

    PersistentDailyOperationQuota(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.quotaPool = null;
    }

    @PreDestroy
    public void close() {
        if (quotaPool != null) quotaPool.close();
    }

    @Override
    public void reserve(String scope, long dailyLimit) {
        if (dailyLimit < 1) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "This operation is disabled by its daily allowance.");
        }
        try {
            var result = jdbc.queryForList("""
                    INSERT INTO daily_operation_quota (scope, usage_date, used)
                    VALUES (?, ?, 1)
                    ON CONFLICT (scope) DO UPDATE
                    SET usage_date = EXCLUDED.usage_date,
                        used = CASE WHEN daily_operation_quota.usage_date = EXCLUDED.usage_date
                                    THEN daily_operation_quota.used + 1 ELSE 1 END
                    WHERE daily_operation_quota.usage_date < EXCLUDED.usage_date
                       OR (daily_operation_quota.usage_date = EXCLUDED.usage_date
                           AND daily_operation_quota.used < ?)
                    RETURNING used
                    """, Long.class, scope, Date.valueOf(LocalDate.now(clock)), dailyLimit);
            if (result.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "Daily allowance reached. Try again tomorrow (UTC).");
            }
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Usage allowance is temporarily unavailable.", ex);
        }
    }
}
