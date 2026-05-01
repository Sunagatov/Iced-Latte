package com.zufar.icedlatte.common.temporarycache;

import java.time.Duration;
import java.util.Optional;

public interface ExpiringKeyValueStore {

    void put(String key, String value, Duration ttl);

    Optional<String> get(String key);

    Optional<String> take(String key);

    void remove(String key);

    boolean contains(String key);
}
