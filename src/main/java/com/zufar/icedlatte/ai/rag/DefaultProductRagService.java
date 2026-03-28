package com.zufar.icedlatte.ai.rag;

import com.zufar.icedlatte.ai.rag.dto.ProductRagAskResponse;
import com.zufar.icedlatte.ai.rag.dto.ProductRagSourceDto;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
class DefaultProductRagService implements ProductRagService {

    private static final String FALLBACK_ANSWER = "AI answer is temporarily unavailable for this product question.";

    private final ProductInfoRepository productInfoRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductRagContextRetriever productRagContextRetriever;
    private final ProductRagAiService productRagAiService;

    @Override
    @Transactional(readOnly = true)
    public ProductRagAskResponse ask(UUID productId, String question) {
        var product = productInfoRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        var reviews = productReviewRepository.findAllByProductId(productId);
        var retrievedContext = productRagContextRetriever.retrieve(product, reviews, question);
        var prompt = buildPrompt(product.getName(), product.getBrandName(), product.getSellerName(), question, retrievedContext);

        String answer;
        try {
            answer = productRagAiService.answer(prompt);
        } catch (Exception exception) {
            log.warn("product.rag.answer.unavailable: productId={} cause={}", productId, exception.getMessage());
            answer = FALLBACK_ANSWER;
        }

        var sources = retrievedContext.stream()
                .map(context -> new ProductRagSourceDto(
                        context.sourceType(),
                        context.sourceLabel(),
                        context.content(),
                        context.score()))
                .toList();

        return new ProductRagAskResponse(productId, product.getName(), question, answer, sources);
    }

    private String buildPrompt(String productName,
                               String brandName,
                               String sellerName,
                               String question,
                               java.util.List<RetrievedProductContext> retrievedContext) {
        var contextBlock = retrievedContext.stream()
                .map(RetrievedProductContext::promptBlock)
                .collect(Collectors.joining("\n\n"));

        return """
                Product name: %s
                Brand: %s
                Seller: %s

                User question:
                %s

                Retrieved context:
                %s

                Instructions:
                - Answer only from the retrieved context above.
                - If the context is not enough, say that clearly.
                - Do not invent missing product facts.
                - Keep the answer concise and practical.
                """.formatted(productName, brandName, sellerName, question, contextBlock);
    }
}
