package com.zufar.icedlatte.email.api.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TokenGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final String BASE = "0123456789";
    private static final String PATTERN = "###-###-###";
    private static final char REPLACE_HOOK = '#';

    public String nextToken() {
        PatternReplacer token = new PatternReplacer(PATTERN, REPLACE_HOOK);
        while (token.isReplaceable()) {
            int randomSymbolIndex = random.nextInt(BASE.length());
            token.replace(BASE.charAt(randomSymbolIndex));
        }
        return token.toString();
    }
}
