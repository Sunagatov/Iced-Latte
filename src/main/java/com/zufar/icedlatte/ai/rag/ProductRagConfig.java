package com.zufar.icedlatte.ai.rag;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
class ProductRagConfig {

    @Bean
    ProductRagAiService productRagAiService(OpenAiChatModel model) {
        return AiServices.builder(ProductRagAiService.class)
                .chatModel(model)
                .build();
    }
}
