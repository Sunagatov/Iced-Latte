package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.review.ai.moderation.ReviewModerationService;
import com.zufar.icedlatte.review.ai.summary.ProductSummaryService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "false", matchIfMissing = true)
class AiDisabledConfig {

    @Bean
    ReviewModerationService noOpModerationService() {
        return _ -> log.debug("ai.moderation.skipped: ai.enabled=false");
    }

    @Bean
    ProductSummaryService noOpProductSummaryService() {
        return _ -> null;
    }
}
