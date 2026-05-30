package com.zufar.icedlatte.product.service;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReviewProductGateway implements ProductReviewProductApi {

    private final ProductInfoRepository productInfoRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean exists(final UUID productId) {
        return productInfoRepository.existsByIdAndActiveTrue(productId);
    }

    @Override
    @CacheEvict(cacheNames = "productById", key = "#productId")
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshReviewAggregates(final UUID productId) {
        productInfoRepository.updateAverageRating(productId);
        productInfoRepository.updateReviewsCount(productId);
    }

    @Override
    @CacheEvict(cacheNames = "productById", allEntries = true)
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshAllReviewAggregates() {
        productInfoRepository.updateAllAverageRatings();
        productInfoRepository.updateAllReviewsCounts();
    }

    @Override
    @CacheEvict(cacheNames = "productById", key = "#productId")
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void updateAiSummary(final UUID productId, final String summary) {
        productInfoRepository.findById(productId).ifPresent(product -> {
            product.setAiSummary(summary);
            productInfoRepository.save(product);
        });
    }
}
