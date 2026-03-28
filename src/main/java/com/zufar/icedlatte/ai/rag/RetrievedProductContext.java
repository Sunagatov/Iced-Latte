package com.zufar.icedlatte.ai.rag;

record RetrievedProductContext(String sourceType, String sourceLabel, String content, double score) {

    RetrievedProductContext truncatedTo(int maxChars) {
        if (content == null || content.length() <= maxChars || maxChars < 4) {
            return this;
        }
        var shortened = content.substring(0, maxChars - 3).stripTrailing() + "...";
        return new RetrievedProductContext(sourceType, sourceLabel, shortened, score);
    }

    String promptBlock() {
        return "[" + sourceLabel + "]\n" + content;
    }
}
