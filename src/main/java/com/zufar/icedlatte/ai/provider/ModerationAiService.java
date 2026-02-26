package com.zufar.icedlatte.ai.provider;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
interface ModerationAiService {

    @SystemMessage("""
            You are a content moderator for a coffee marketplace.
            Analyze the text and determine if it contains harassment, hate speech, spam, terrorism, bullying, or other inappropriate content.
            Reply with only OK if the content is acceptable, or NOT_OK: <reason> if it violates guidelines.
            """)
    String moderate(@UserMessage String text);
}
