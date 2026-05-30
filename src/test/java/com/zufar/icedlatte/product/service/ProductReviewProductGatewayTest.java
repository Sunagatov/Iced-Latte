package com.zufar.icedlatte.product.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.cache.annotation.CacheEvict;

import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewProductGateway unit tests")
class ProductReviewProductGatewayTest {

    @Mock
    private ProductInfoRepository productInfoRepository;

    @InjectMocks
    private ProductReviewProductGateway gateway;

    @Test
    @DisplayName("exists only reports active products")
    void existsUsesActiveProductLookup() {
        UUID productId = UUID.randomUUID();
        when(productInfoRepository.existsByIdAndActiveTrue(productId)).thenReturn(true);

        gateway.exists(productId);

        verify(productInfoRepository).existsByIdAndActiveTrue(productId);
    }

    @Test
    @DisplayName("refreshReviewAggregates updates both aggregate fields")
    void refreshReviewAggregatesUpdatesBothAggregateFields() {
        UUID productId = UUID.randomUUID();

        gateway.refreshReviewAggregates(productId);

        verify(productInfoRepository).updateAverageRating(productId);
        verify(productInfoRepository).updateReviewsCount(productId);
    }

    @Test
    @DisplayName("refreshAllReviewAggregates updates all aggregate fields")
    void refreshAllReviewAggregatesUpdatesAllAggregateFields() {
        gateway.refreshAllReviewAggregates();

        verify(productInfoRepository).updateAllAverageRatings();
        verify(productInfoRepository).updateAllReviewsCounts();
    }

    @Test
    @DisplayName("updateAiSummary persists a product summary when the product exists")
    void updateAiSummaryPersistsProductSummary() {
        UUID productId = UUID.randomUUID();
        ProductInfo product = new ProductInfo();
        when(productInfoRepository.findById(productId)).thenReturn(Optional.of(product));

        gateway.updateAiSummary(productId, "summary");

        verify(productInfoRepository).save(product);
    }

    @Test
    @DisplayName("product cache is evicted when a single product aggregate changes")
    void refreshReviewAggregatesEvictsProductCache() throws NoSuchMethodException {
        CacheEvict cacheEvict = ProductReviewProductGateway.class
                .getMethod("refreshReviewAggregates", UUID.class)
                .getAnnotation(CacheEvict.class);

        assertThat(cacheEvict.cacheNames()).containsExactly("productById");
        assertThat(cacheEvict.key()).isEqualTo("#productId");
    }

    @Test
    @DisplayName("product cache is cleared when all product aggregates change")
    void refreshAllReviewAggregatesClearsProductCache() throws NoSuchMethodException {
        CacheEvict cacheEvict = ProductReviewProductGateway.class
                .getMethod("refreshAllReviewAggregates")
                .getAnnotation(CacheEvict.class);

        assertThat(cacheEvict.cacheNames()).containsExactly("productById");
        assertThat(cacheEvict.allEntries()).isTrue();
    }
}
