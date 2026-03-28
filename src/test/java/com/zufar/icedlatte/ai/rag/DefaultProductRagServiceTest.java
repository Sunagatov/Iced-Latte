package com.zufar.icedlatte.ai.rag;

import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultProductRagService unit tests")
class DefaultProductRagServiceTest {

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private ProductRagContextRetriever productRagContextRetriever;

    @Mock
    private ProductRagAiService productRagAiService;

    @InjectMocks
    private DefaultProductRagService service;

    @Test
    @DisplayName("ask: returns grounded answer with retrieved sources")
    void ask_success_returnsAnswerAndSources() {
        var productId = UUID.randomUUID();
        var product = new ProductInfo();
        product.setId(productId);
        product.setName("Colombia Filter Roast");
        product.setBrandName("Iced Latte Roasters");
        product.setSellerName("Iced Latte");

        var retrievedContext = List.of(
                new RetrievedProductContext("PRODUCT_DETAILS", "Product details", "Chocolate and caramel notes.", 55.0),
                new RetrievedProductContext("REVIEW", "Review #1 — rating 5/5", "Review text: sweet and smooth.", 25.0)
        );

        when(productInfoRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productReviewRepository.findAllByProductId(productId)).thenReturn(List.of());
        when(productRagContextRetriever.retrieve(product, List.of(), "Is it sweet?"))
                .thenReturn(retrievedContext);
        when(productRagAiService.answer(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Yes. The retrieved context describes chocolate, caramel, and a sweet smooth cup.");

        var response = service.ask(productId, "Is it sweet?");

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.answer()).contains("sweet");
        assertThat(response.sources()).hasSize(2);
        assertThat(response.sources().getFirst().sourceLabel()).isEqualTo("Product details");
    }

    @Test
    @DisplayName("ask: throws ProductNotFoundException when product does not exist")
    void ask_missingProduct_throwsProductNotFoundException() {
        var productId = UUID.randomUUID();
        when(productInfoRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ask(productId, "Tell me about it"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(productId.toString());
    }
}
