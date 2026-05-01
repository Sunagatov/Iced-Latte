package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductReviewProductGateway {

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
