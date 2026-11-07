package com.zufar.icedlatte.common.sync;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyncRevisionService {
    private final JdbcTemplate jdbc;

    @Transactional
    public long lock(UUID userId, String resource) {
        jdbc.update(
                "INSERT INTO sync_resource_state (user_id, resource) VALUES (?, ?) ON CONFLICT DO NOTHING",
                userId,
                resource);
        return jdbc.queryForObject(
                "SELECT revision FROM sync_resource_state WHERE user_id = ? AND resource = ? FOR UPDATE",
                Long.class,
                userId,
                resource);
    }

    public long current(UUID userId, String resource) {
        return jdbc
                .query(
                        "SELECT revision FROM sync_resource_state WHERE user_id = ? AND resource = ?",
                        (rs, row) -> rs.getLong(1),
                        userId,
                        resource)
                .stream()
                .findFirst()
                .orElse(0L);
    }

    @Transactional
    public void changed(UUID userId, String resource) {
        lock(userId, resource);
        jdbc.update(
                "UPDATE sync_resource_state SET revision = revision + 1 WHERE user_id = ? AND resource = ?",
                userId,
                resource);
    }
}
