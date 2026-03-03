package com.zufar.icedlatte.review.ai;

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
        return text -> log.debug("ai.moderation.skipped: ai.enabled=false");
    }

    @Bean
    ProductSummaryService noOpProductSummaryService() {
        return productId -> null;
    }
}
