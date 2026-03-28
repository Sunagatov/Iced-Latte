package com.zufar.icedlatte.ai.rag.dto;

public record ProductRagSourceDto(
        String sourceType,
        String sourceLabel,
        String excerpt,
        double score
) {
}
