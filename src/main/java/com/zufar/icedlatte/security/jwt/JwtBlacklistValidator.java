package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtBlacklistValidator {

    @Autowired(required = false)
    private RedisJwtBlacklistService redisJwtBlacklistService;
    
    @Autowired(required = false)
    private InMemoryJwtBlacklistService inMemoryJwtBlacklistService;

    public void addToBlacklist(String token) {
        if (redisJwtBlacklistService != null) {
            redisJwtBlacklistService.blacklistToken(token);
        } else if (inMemoryJwtBlacklistService != null) {
            inMemoryJwtBlacklistService.blacklistToken(token);
        } else {
            log.error("No blacklist service available - token cannot be blacklisted");
        }
    }

    public void validate(String token) {
        boolean isBlacklisted;
        
        if (redisJwtBlacklistService != null) {
            isBlacklisted = redisJwtBlacklistService.isBlacklisted(token);
        } else if (inMemoryJwtBlacklistService != null) {
            isBlacklisted = inMemoryJwtBlacklistService.isBlacklisted(token);
        } else {
            log.error("No blacklist service available - failing secure");
            throw new JwtTokenBlacklistedException();
        }
        
        if (isBlacklisted) {
            throw new JwtTokenBlacklistedException();
        }
    }
}
