package com.zufar.icedlatte.ratelimit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class PersistentDailyOperationQuotaMetricsTest {
    @Test
    void quotaOutcomesUseBoundedCategoriesWithoutAccountIds() {
        var jdbc = mock(JdbcTemplate.class);
        var registry = new SimpleMeterRegistry();
        var quota = new PersistentDailyOperationQuota(jdbc, Clock.systemUTC(), registry);
        when(jdbc.queryForList(anyString(), eq(Long.class), any(), any(), any()))
                .thenReturn(List.of(1L));
        quota.reserve("avatar:user:private-account", 10);
        assertThat(registry.get("operation.quota.decisions")
                        .tags("category", "uploads", "outcome", "allowed")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(
                        meter -> assertThat(meter.getId().getTags().toString()).doesNotContain("private-account"));
        assertThatThrownBy(() -> quota.reserve("ai:reviews", 0)).isInstanceOf(ResponseStatusException.class);
        assertThat(registry.get("operation.quota.decisions")
                        .tags("category", "ai", "outcome", "blocked")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void unavailableQuotaIsCountedAndStillFailsClosed() {
        var jdbc = mock(JdbcTemplate.class);
        var registry = new SimpleMeterRegistry();
        when(jdbc.queryForList(anyString(), eq(Long.class), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));
        var quota = new PersistentDailyOperationQuota(jdbc, Clock.systemUTC(), registry);
        assertThatThrownBy(() -> quota.reserve("ai:reviews", 10)).isInstanceOf(ResponseStatusException.class);
        assertThat(registry.get("operation.quota.decisions")
                        .tags("category", "ai", "outcome", "unavailable")
                        .counter()
                        .count())
                .isEqualTo(1);
    }
}
