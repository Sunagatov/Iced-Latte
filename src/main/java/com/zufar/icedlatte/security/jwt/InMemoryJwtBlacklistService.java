package com.zufar.icedlatte.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnMissingBean(RedisJwtBlacklistService.class)
public class InMemoryJwtBlacklistService {

    private final Set<String> blacklistedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
        log.debug("Token blacklisted in memory");
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}