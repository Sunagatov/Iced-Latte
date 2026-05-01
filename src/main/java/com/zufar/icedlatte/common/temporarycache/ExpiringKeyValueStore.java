package com.zufar.icedlatte.common.temporarycache;

import java.time.Duration;
import java.util.Optional;

public interface ExpiringKeyValueStore {

    void put(String key, Object value, Duration ttl);

    <T> Optional<T> get(String key, Class<T> valueType);

    <T> Optional<T> take(String key, Class<T> valueType);

    void remove(String key);

    boolean contains(String key);
}
