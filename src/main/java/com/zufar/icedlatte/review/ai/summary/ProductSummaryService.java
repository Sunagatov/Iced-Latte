package com.zufar.icedlatte.review.ai.summary;

import java.util.UUID;

public interface ProductSummaryService {

    String summarize(UUID productId);
}
