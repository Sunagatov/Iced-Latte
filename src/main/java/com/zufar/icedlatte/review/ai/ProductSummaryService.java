package com.zufar.icedlatte.review.ai;

import java.util.UUID;

public interface ProductSummaryService {
    String summarize(UUID productId);
}
