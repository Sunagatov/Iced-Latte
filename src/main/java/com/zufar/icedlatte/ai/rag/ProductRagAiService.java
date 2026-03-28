package com.zufar.icedlatte.ai.rag;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

interface ProductRagAiService {

    @SystemMessage("""
            You are a product assistant for the Iced Latte coffee marketplace.
            Answer only from the retrieved product context you receive.
            If the context is insufficient, say clearly that the answer is not available from the provided context.
            Do not invent facts, specifications, prices, ingredients, or review sentiment.
            Keep the answer practical and concise.
            """)
    String answer(@UserMessage String prompt);
}
