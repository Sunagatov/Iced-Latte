package com.zufar.icedlatte.email.api;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GenerateToken {
    private final SecureRandom random = new SecureRandom();
    private final String BASE = "0123456789";
    private final String PATTERN = "###-###";
    private final char REPLACE_HOOK = '#';

    public String nextToken() {
        PatternReplacer token = new PatternReplacer(PATTERN, REPLACE_HOOK);
        while (token.isReplaceable()) {
            int randomSymbolIndex = random.nextInt(BASE.length());
            token.replace(BASE.charAt(randomSymbolIndex));
        }
        return token.toString();
    }
}
