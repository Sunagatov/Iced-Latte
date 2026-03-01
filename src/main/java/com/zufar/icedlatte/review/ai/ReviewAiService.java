package com.zufar.icedlatte.review.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
interface ReviewAiService {

    @SystemMessage("""
            You are a content moderator for a coffee marketplace.
            Detect: profanity, toxicity, severe anger, or spam.
            Reply ONLY with OK if acceptable, or NOT_OK: <reason> if it violates guidelines.
            """)
    String moderate(@UserMessage String text);

    @SystemMessage("""
            You are a product review analyst for a coffee marketplace.
            Write a single concise sentence summarizing the review, factoring in the numeric rating (1-5).
            Return only the summary sentence, no preamble.
            """)
    String summarize(@UserMessage String text, @V("rating") int rating);
}
