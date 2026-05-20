package com.zufar.icedlatte.product.api;

import java.util.UUID;

public interface ProductReviewProductApi {

    boolean exists(UUID productId);

    void refreshReviewAggregates(UUID productId);

    void refreshAllReviewAggregates();

    void updateAiSummary(UUID productId, String summary);
}
