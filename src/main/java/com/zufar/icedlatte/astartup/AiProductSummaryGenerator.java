package com.zufar.icedlatte.astartup;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-shot runner — run once locally, never on app startup.
 *
 * Usage:
 *   export SPRING_AI_OPENAI_API_KEY=sk-...
 *   mvn exec:java -Dexec.mainClass="com.zufar.icedlatte.astartup.AiProductSummaryGenerator"
 *
 * Redirect stdout to a migration file:
 *   mvn exec:java -Dexec.mainClass="com.zufar.icedlatte.astartup.AiProductSummaryGenerator" \
 *     > src/main/resources/db/changelog/version-1.0/01.07.2026.part8.insert-ai-summaries.sql
 */
public class AiProductSummaryGenerator {

    private static final String API_KEY_ENV = "SPRING_AI_OPENAI_API_KEY";
    private static final String BASE_URL    = "https://models.inference.ai.azure.com";
    private static final String MODEL       = "gpt-4o-mini";

    // All review SQL files relative to project root
    private static final List<String> REVIEW_FILES = List.of(
        "src/main/resources/db/changelog/version-1.0/08.02.2024.part2.insert-product-review-data.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part3.insert-new-product-review-data.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part4.insert-extra-reviews.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part5.insert-extra-reviews-batch2.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part6.insert-extra-reviews-batch3.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part7.insert-extra-reviews-batch4.sql"
    );

    interface SummaryService {
        @SystemMessage("""
                You are a product review analyst for a coffee marketplace.
                Given a list of customer reviews, write a single concise sentence summarizing the overall sentiment.
                Start with "Customers". Return only the summary sentence, no preamble.
                """)
        String summarize(@UserMessage String reviews);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("local")) {
            System.err.println("Set " + API_KEY_ENV + " env var to a real OpenAI key.");
            System.exit(1);
        }

        var model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(BASE_URL)
                .modelName(MODEL)
                .temperature(0.0)
                .build();

        SummaryService ai = AiServices.builder(SummaryService.class)
                .chatModel(model)
                .build();

        // product_id -> list of review texts
        Map<String, StringBuilder> reviewsByProduct = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile(
            "VALUES\\s*\\('[^']+',\\s*'([^']+)',\\s*'[^']+',\\s*'((?:[^']|'')*)',\\s*(\\d)"
        );

        for (String file : REVIEW_FILES) {
            String sql = Files.readString(Path.of(file));
            Matcher m = pattern.matcher(sql);
            while (m.find()) {
                String productId = m.group(1);
                String text      = m.group(2).replace("''", "'");
                reviewsByProduct
                    .computeIfAbsent(productId, k -> new StringBuilder())
                    .append("- ").append(text).append("\n");
            }
        }

        // Load already-done product IDs from existing output file (if any)
        Path outFile = Path.of("src/main/resources/db/changelog/version-1.0/01.07.2026.part8.insert-ai-summaries.sql");
        java.util.Set<String> done = new java.util.HashSet<>();
        if (Files.exists(outFile)) {
            Pattern donePattern = Pattern.compile("WHERE id = '([^']+)'");
            for (String line : Files.readAllLines(outFile)) {
                Matcher dm = donePattern.matcher(line);
                if (dm.find()) done.add(dm.group(1));
            }
        } else {
            Files.writeString(outFile, "-- AI-generated product summaries — generated once, never regenerated\n-- Run: AiProductSummaryGenerator\n\n");
        }
        if (!done.isEmpty()) System.err.println("Skipping " + done.size() + " already summarised products.");

        for (var entry : reviewsByProduct.entrySet()) {
            String productId = entry.getKey();
            if (done.contains(productId)) continue;
            String reviews = entry.getValue().toString();
            System.err.println("Summarising product " + productId + " ...");
            String summary = null;
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    summary = ai.summarize(reviews).replace("'", "''");
                    break;
                } catch (Exception e) {
                    System.err.println("  attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt < 5) Thread.sleep(5000L * attempt);
                }
            }
            if (summary == null) { System.err.println("  SKIPPED " + productId); continue; }
            String line = "UPDATE product SET ai_summary = '" + summary + "' WHERE id = '" + productId + "';\n";
            Files.writeString(outFile, line, java.nio.file.StandardOpenOption.APPEND);
        }
    }
}
