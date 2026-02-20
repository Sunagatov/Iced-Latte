package com.zufar.icedlatte.security.jwt;

public interface JwtBlacklistService {
    void blacklistToken(String token);
    boolean isBlacklisted(String token);
}
