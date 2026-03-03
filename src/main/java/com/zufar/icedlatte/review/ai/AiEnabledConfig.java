package com.zufar.icedlatte.review.ai;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
class AiEnabledConfig {

    @Bean
    OpenAiChatModel openAiChatModel(
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.model-name:gpt-4o-mini}") String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    @Bean
    ReviewAiService reviewAiService(OpenAiChatModel model) {
        return AiServices.builder(ReviewAiService.class)
                .chatModel(model)
                .build();
    }
}
