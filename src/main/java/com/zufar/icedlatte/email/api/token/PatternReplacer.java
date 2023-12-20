package com.zufar.icedlatte.email.api.token;

import java.util.ArrayList;
import java.util.List;

public class PatternReplacer {

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
