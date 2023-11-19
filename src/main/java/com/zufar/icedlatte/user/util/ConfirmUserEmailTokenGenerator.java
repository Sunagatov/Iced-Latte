package com.zufar.icedlatte.user.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class ConfirmUserEmailTokenGenerator {

    public static final String NUMERIC_BASE = "0123456789";
    public static final String LATIN_BASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final String DEFAULT_BASE = NUMERIC_BASE + LATIN_BASE;

    private final SecureRandom random = new SecureRandom();

    public String nextToken(int tokenLength) {
        StringBuilder token = new StringBuilder(tokenLength);
        for (int i = 0; i < tokenLength; i++) {
            int randomSymbolIndex = random.nextInt(DEFAULT_BASE.length());
            token.append(DEFAULT_BASE.charAt(randomSymbolIndex));
        }
        return token.toString();
    }

    public String nextToken(String base, int tokenLength) {
        StringBuilder token = new StringBuilder(tokenLength);
        for (int i = 0; i < tokenLength; i++) {
            int randomSymbolIndex = random.nextInt(base.length());
            token.append(base.charAt(randomSymbolIndex));
        }
        return token.toString();
    }

    /**
     * Example,
     * Base is "0123456789"
     * Replace hook is '#'
     * Pattern is "###-###"
     * Instead of '#', generated symbols are substituted and for example, random result will be 143-376
     * Example,
     * Base is "ABCDEFG"
     * Replace hook is '?'
     * Pattern is "??-??-??"
     * Instead of '?', generated symbols are substituted and for example, random result will be FD-GA-AB
     */
    public String nextToken(String base, String pattern, char replaceHook) {
        PatternReplacer token = new PatternReplacer(pattern, replaceHook);
        while (token.isReplaceable()) {
            int randomSymbolIndex = random.nextInt(base.length());
            token.replace(base.charAt(randomSymbolIndex));
        }
        return token.toString();
    }

    static class PatternReplacer {

        private final List<Integer> replacingIndexes;
        private final StringBuilder replacedPattern;
        private int replacedSymbolsCount;

        public PatternReplacer(String pattern, Character replaceHook) {
            replacedPattern = new StringBuilder(pattern);
            replacingIndexes = new ArrayList<>();
            replacedSymbolsCount = 0;
            parsePattern(pattern, replaceHook);
        }

        private void parsePattern(String pattern, Character replaceHook) {
            int index = pattern.indexOf(replaceHook);
            while (index >= 0) {
                replacingIndexes.add(index);
                index = pattern.indexOf(replaceHook, index + 1);
            }
        }

        public void replace(char replacingSymbol) {
            Integer replacingIndex = replacingIndexes.get(replacedSymbolsCount);
            replacedPattern.setCharAt(replacingIndex, replacingSymbol);
            replacedSymbolsCount++;
        }

        public boolean isReplaceable() {
            return replacedSymbolsCount < replacingIndexes.size();
        }

        @Override
        public String toString() {
            return replacedPattern.toString();
        }
    }
}