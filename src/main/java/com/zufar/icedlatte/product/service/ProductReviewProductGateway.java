package com.zufar.icedlatte.product.service;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReviewProductGateway implements ProductReviewProductApi {

    private final ProductInfoRepository productInfoRepository;

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, isolation = Isolation.READ_COMMITTED)
    public boolean exists(final UUID productId) {
        return productInfoRepository.existsById(productId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshReviewAggregates(final UUID productId) {
        productInfoRepository.updateAverageRating(productId);
        productInfoRepository.updateReviewsCount(productId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshAllReviewAggregates() {
        productInfoRepository.updateAllAverageRatings();
        productInfoRepository.updateAllReviewsCounts();
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void updateAiSummary(final UUID productId, final String summary) {
        productInfoRepository.findById(productId).ifPresent(product -> {
            product.setAiSummary(summary);
            productInfoRepository.save(product);
        });
    }
}
