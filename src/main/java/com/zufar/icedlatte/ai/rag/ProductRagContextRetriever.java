package com.zufar.icedlatte.ai.rag;

import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.review.entity.ProductReview;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class ProductRagContextRetriever {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "into", "about", "your", "their",
            "does", "what", "when", "where", "which", "have", "has", "had", "are", "was", "were",
            "but", "not", "too", "can", "how", "you", "its", "they", "them", "than", "then"
    );

    private final int maxContextItems;
    private final int maxContextCharsPerItem;

    ProductRagContextRetriever(
            @Value("${ai.rag.max-context-items:5}") int maxContextItems,
            @Value("${ai.rag.max-context-chars-per-item:500}") int maxContextCharsPerItem) {
        this.maxContextItems = maxContextItems;
        this.maxContextCharsPerItem = maxContextCharsPerItem;
    }

    List<RetrievedProductContext> retrieve(ProductInfo product, List<ProductReview> reviews, String question) {
        var questionTokens = tokenize(question);
        var candidates = new ArrayList<RetrievedProductContext>();

        candidates.add(buildProductDetailsContext(product, question, questionTokens));

        if (StringUtils.isNotBlank(product.getAiSummary())) {
            candidates.add(buildContext(
                    "PRODUCT_AI_SUMMARY",
                    "Product AI summary",
                    product.getAiSummary(),
                    question,
                    questionTokens,
                    30.0
            ));
        }

        var sortedReviews = reviews.stream()
                .sorted(Comparator
                        .comparingInt((ProductReview review) -> review.getLikesCount() == null ? 0 : review.getLikesCount())
                        .reversed()
                        .thenComparing(ProductReview::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .toList();

        for (int i = 0; i < sortedReviews.size(); i++) {
            var review = sortedReviews.get(i);
            var label = "Review #" + (i + 1) + " — rating " + review.getProductRating() + "/5";
            var content = "Rating: " + review.getProductRating() + "/5. Review text: " + review.getText();
            candidates.add(buildContext("REVIEW", label, content, question, questionTokens, 10.0));
        }

        return candidates.stream()
                .filter(candidate -> StringUtils.isNotBlank(candidate.content()))
                .sorted(Comparator
                        .comparingDouble(RetrievedProductContext::score)
                        .reversed()
                        .thenComparing(RetrievedProductContext::sourceLabel))
                .limit(maxContextItems)
                .map(candidate -> candidate.truncatedTo(maxContextCharsPerItem))
                .toList();
    }

    private RetrievedProductContext buildProductDetailsContext(ProductInfo product, String question, Set<String> questionTokens) {
        var details = String.format(
                "Product name: %s. Description: %s. Brand: %s. Seller: %s. Origin country: %s. Average rating: %s. Reviews count: %s.",
                product.getName(),
                product.getDescription(),
                product.getBrandName(),
                product.getSellerName(),
                product.getOriginCountry(),
                product.getAverageRating(),
                product.getReviewsCount()
        );
        return buildContext("PRODUCT_DETAILS", "Product details", details, question, questionTokens, 40.0);
    }

    private RetrievedProductContext buildContext(String sourceType,
                                                 String sourceLabel,
                                                 String content,
                                                 String question,
                                                 Set<String> questionTokens,
                                                 double baseScore) {
        var score = score(question, questionTokens, content, baseScore);
        return new RetrievedProductContext(sourceType, sourceLabel, content, score);
    }

    private double score(String question, Set<String> questionTokens, String candidateText, double baseScore) {
        var normalizedQuestion = StringUtils.defaultString(question).toLowerCase(Locale.ROOT).trim();
        var candidateTokens = tokenize(candidateText);

        var overlap = questionTokens.stream()
                .filter(candidateTokens::contains)
                .count();

        var score = baseScore + (overlap * 15.0);
        var loweredCandidate = StringUtils.defaultString(candidateText).toLowerCase(Locale.ROOT);

        if (StringUtils.isNotBlank(normalizedQuestion) && loweredCandidate.contains(normalizedQuestion)) {
            score += 25.0;
        }

        for (var token : questionTokens) {
            if (loweredCandidate.contains(token)) {
                score += 1.0;
            }
        }

        return score;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(StringUtils.defaultString(text).toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(StringUtils::isNotBlank)
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
