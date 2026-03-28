package com.zufar.icedlatte.ai.rag;

import com.zufar.icedlatte.ai.rag.dto.ProductRagAskResponse;

import java.util.UUID;

public interface ProductRagService {

    ProductRagAskResponse ask(UUID productId, String question);
}
