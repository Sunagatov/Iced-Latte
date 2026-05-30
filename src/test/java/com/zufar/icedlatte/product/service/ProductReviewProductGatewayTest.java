package com.zufar.icedlatte.product.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewProductGateway unit tests")
class ProductReviewProductGatewayTest {

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private ProductCacheEvictor productCacheEvictor;

    @InjectMocks
    private ProductReviewProductGateway gateway;

    @Test
    @DisplayName("exists delegates to repository")
    void existsDelegatesToRepository() {
        UUID productId = UUID.randomUUID();
        when(productInfoRepository.existsById(productId)).thenReturn(true);

        gateway.exists(productId);

        verify(productInfoRepository).existsById(productId);
    }

    @Test
    @DisplayName("refreshReviewAggregates updates both aggregate fields")
    void refreshReviewAggregatesUpdatesBothAggregateFields() {
        UUID productId = UUID.randomUUID();

        gateway.refreshReviewAggregates(productId);

        verify(productInfoRepository).updateAverageRating(productId);
        verify(productInfoRepository).updateReviewsCount(productId);
        verify(productCacheEvictor).evictProductByIdAfterCommit(productId);
    }

    @Test
    @DisplayName("refreshAllReviewAggregates updates all aggregate fields")
    void refreshAllReviewAggregatesUpdatesAllAggregateFields() {
        gateway.refreshAllReviewAggregates();

        verify(productInfoRepository).updateAllAverageRatings();
        verify(productInfoRepository).updateAllReviewsCounts();
        verify(productCacheEvictor).clearProductByIdAfterCommit();
    }

    @Test
    @DisplayName("updateAiSummary persists a product summary when the product exists")
    void updateAiSummaryPersistsProductSummary() {
        UUID productId = UUID.randomUUID();
        ProductInfo product = new ProductInfo();
        when(productInfoRepository.findById(productId)).thenReturn(Optional.of(product));

        gateway.updateAiSummary(productId, "summary");

        verify(productInfoRepository).save(product);
        verify(productCacheEvictor).evictProductByIdAfterCommit(productId);
    }
}
