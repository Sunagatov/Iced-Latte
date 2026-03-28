package com.zufar.icedlatte.ai.rag.endpoint;

import com.zufar.icedlatte.ai.rag.ProductRagService;
import com.zufar.icedlatte.ai.rag.dto.ProductRagAskRequest;
import com.zufar.icedlatte.ai.rag.dto.ProductRagAskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@RequestMapping(ProductRagEndpoint.PRODUCT_RAG_URL)
public class ProductRagEndpoint {

    public static final String PRODUCT_RAG_URL = "/api/v1/products/{productId}/rag";

    private final ProductRagService productRagService;

    @PostMapping("/ask")
    public ResponseEntity<ProductRagAskResponse> askQuestion(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRagAskRequest request) {
        log.info("product.rag.ask: productId={} questionLength={}", productId, request.question().length());
        return ResponseEntity.ok(productRagService.ask(productId, request.question()));
    }
}
