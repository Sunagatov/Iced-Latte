package com.zufar.icedlatte.astartup;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
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

    private static final Logger log = LoggerFactory.getLogger(AiProductSummaryGenerator.class);

    private static final String API_KEY_ENV = "SPRING_AI_OPENAI_API_KEY";
    private static final String BASE_URL    = "https://models.inference.ai.azure.com";
    private static final String MODEL       = "gpt-4o-mini";

    private static final List<String> REVIEW_FILES = List.of(
        "src/main/resources/db/changelog/version-1.0/08.02.2024.part2.insert-product-review-data.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part3.insert-new-product-review-data.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part4.insert-extra-reviews.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part5.insert-extra-reviews-batch2.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part6.insert-extra-reviews-batch3.sql",
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part7.insert-extra-reviews-batch4.sql"
    );

    // Possessive quantifier on inner group prevents catastrophic backtracking
    private static final Pattern REVIEW_PATTERN = Pattern.compile(
        "VALUES\\s*\\('[^']+',\\s*'([^']+)',\\s*'[^']+',\\s*'((?:[^']|'')*+)',\\s*(\\d)"
    );

    private static final Path OUT_FILE = Path.of(
        "src/main/resources/db/changelog/version-1.0/01.07.2026.part8.insert-ai-summaries.sql"
    );

    interface SummaryService {
        @SystemMessage("""
                You are a product review analyst for a coffee marketplace.
                Given a list of customer reviews, write a single concise sentence summarizing the overall sentiment.
                Start with "Customers". Return only the summary sentence, no preamble.
                """)
        String summarize(@UserMessage String reviews);
    }

    public static void main(String[] ignored) throws IOException, InterruptedException {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("local")) {
            log.error("startup.ai.key.missing: envVar={}", API_KEY_ENV);
            System.exit(1);
        }

        SummaryService ai = buildAiService(apiKey);
        Map<String, StringBuilder> reviewsByProduct = collectReviews();
        Set<String> done = loadAlreadyDone();

        if (!done.isEmpty()) {
            log.info("startup.ai.skipping: count={}", done.size());
        }

        for (Map.Entry<String, StringBuilder> entry : reviewsByProduct.entrySet()) {
            processProduct(ai, entry.getKey(), entry.getValue().toString(), done);
        }
    }

    private static SummaryService buildAiService(String apiKey) {
        var model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(BASE_URL)
                .modelName(MODEL)
                .temperature(0.0)
                .build();
        return AiServices.builder(SummaryService.class)
                .chatModel(model)
                .build();
    }

    private static Map<String, StringBuilder> collectReviews() throws IOException {
        Map<String, StringBuilder> reviewsByProduct = new LinkedHashMap<>();
        for (String file : REVIEW_FILES) {
            String sql = Files.readString(Path.of(file));
            Matcher m = REVIEW_PATTERN.matcher(sql);
            while (m.find()) {
                String productId = m.group(1);
                String text      = m.group(2).replace("''", "'");
                reviewsByProduct
                    .computeIfAbsent(productId, k -> new StringBuilder())
                    .append("- ").append(text).append("\n");
            }
        }
        return reviewsByProduct;
    }

    private static Set<String> loadAlreadyDone() throws IOException {
        Set<String> done = new HashSet<>();
        if (!Files.exists(OUT_FILE)) {
            Files.writeString(OUT_FILE, "-- AI-generated product summaries — generated once, never regenerated\n-- Run: AiProductSummaryGenerator\n\n");
            return done;
        }
        Pattern donePattern = Pattern.compile("WHERE id = '([^']+)'");
        for (String line : Files.readAllLines(OUT_FILE)) {
            Matcher dm = donePattern.matcher(line);
            if (dm.find()) {
                done.add(dm.group(1));
            }
        }
        return done;
    }

    private static void processProduct(SummaryService ai, String productId, String reviews, Set<String> done)
            throws InterruptedException, IOException {
        if (done.contains(productId)) {
            return;
        }
        log.info("startup.ai.summarising: productId={}", productId);
        String summary = summariseWithRetry(ai, productId, reviews);
        if (summary == null) {
            log.warn("startup.ai.skipped: productId={}", productId);
            return;
        }
        String line = "UPDATE product SET ai_summary = '" + summary + "' WHERE id = '" + productId + "';\n";
        Files.writeString(OUT_FILE, line, java.nio.file.StandardOpenOption.APPEND);
    }

    private static String summariseWithRetry(SummaryService ai, String productId, String reviews)
            throws InterruptedException {
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                return ai.summarize(reviews).replace("'", "''");
            } catch (Exception e) {
                log.warn("startup.ai.attempt.failed: productId={}, attempt={}, error={}", productId, attempt, e.getMessage());
                if (attempt < 5) {
                    Thread.sleep(5000L * attempt);
                }
            }
        }
        return null;
    }
}
