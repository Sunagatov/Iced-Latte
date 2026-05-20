package com.zufar.icedlatte.review.service.ai.summary;

import java.util.UUID;

public interface ProductSummaryService {

    String summarize(UUID productId);
}
