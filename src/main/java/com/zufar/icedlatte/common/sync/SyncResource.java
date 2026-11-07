package com.zufar.icedlatte.common.sync;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public interface SyncResource {
    record Conflict(String productId, String code, String message) {}

    String name();

    Object snapshot(UUID userId);

    List<Conflict> validate(UUID userId, String type, JsonNode payload);

    void apply(UUID userId, String type, JsonNode payload);
}
