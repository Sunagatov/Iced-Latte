package com.zufar.icedlatte.openai.api;

import com.zufar.icedlatte.openai.exception.ChatServiceUnavailableException;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class ProductReviewOpenAiValidator {

    private final OpenAiChatModel chatModel;

    @Value("${spring.ai.openai.prompt}")
    private String openAiPrompt;

    @Autowired
    public ProductReviewOpenAiValidator(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

@GetMapping("/ai/generate")
@Validated
public Boolean isProductReviewContentValid(@RequestParam("review") @javax.validation.constraints.Size(max=1000) String review) {

    PromptTemplate promptTemplate = new PromptTemplate(openAiPrompt);
    promptTemplate.add("review", review);

    ChatResponse response;
    try {
        response = chatModel.call(new Prompt(List.of(promptTemplate.createMessage()),
                OpenAiChatOptions.builder().build()));
    } catch (RuntimeException e) {
        throw new ChatServiceUnavailableException("Chat service is unavailable", e);
    }
    return response.getResults().stream()
            .anyMatch(result -> !result.getOutput().getContent().startsWith("not appropriate"));
}

    private String extractReason(String content) {
        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown reason";
    }
}
