package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtBlacklistValidator {

    private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());

    public void addToBlacklist(String token) {
        blacklistedTokens.add(token);
    }

    public void validate(String token) {
        if (blacklistedTokens.contains(token)) {
            throw new JwtTokenBlacklistedException();
        }
    }
}
