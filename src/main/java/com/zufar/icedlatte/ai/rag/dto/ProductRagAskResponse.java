package com.zufar.icedlatte.ai.rag.dto;

import java.util.List;
import java.util.UUID;

public record ProductRagAskResponse(
        UUID productId,
        String productName,
        String question,
        String answer,
        List<ProductRagSourceDto> sources
) {
}
