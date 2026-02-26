package com.zufar.icedlatte.ai.api;

/**
 * Provider-agnostic contract for AI-based content moderation.
 * Implementations may use any LLM provider (OpenAI, Gemini, etc.).
 */
public interface AiModerationService {

    /**
     * Checks whether the given text complies with community guidelines.
     *
     * @throws com.zufar.icedlatte.openai.exception.InappropriateContentException if content violates guidelines
     */
    void moderate(String text);
}
