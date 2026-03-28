package com.zufar.icedlatte.ai.rag;

import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.review.entity.ProductReview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductRagContextRetriever unit tests")
class ProductRagContextRetrieverTest {

    private final ProductRagContextRetriever retriever = new ProductRagContextRetriever(3, 200);

    @Test
    @DisplayName("retrieve: prioritizes product contexts matching the user question")
    void retrieve_matchingQuestion_prioritizesRelevantContexts() {
        var product = buildProduct();
        product.setAiSummary("Customers describe a smooth espresso with chocolate notes and very low acidity.");

        var firstReview = new ProductReview();
        firstReview.setProductRating(5);
        firstReview.setLikesCount(7);
        firstReview.setCreatedAt(OffsetDateTime.now());
        firstReview.setText("Very chocolatey, smooth, and easy to drink.");

        var secondReview = new ProductReview();
        secondReview.setProductRating(3);
        secondReview.setLikesCount(1);
        secondReview.setCreatedAt(OffsetDateTime.now().minusDays(1));
        secondReview.setText("More citrus than chocolate for my taste.");

        var result = retriever.retrieve(product, List.of(firstReview, secondReview), "Does it taste chocolatey?");

        assertThat(result).hasSize(3);
        assertThat(result.getFirst().content().toLowerCase()).contains("chocolate");
        assertThat(result).anyMatch(context -> context.sourceType().equals("PRODUCT_AI_SUMMARY"));
    }

    @Test
    @DisplayName("retrieve: keeps result size within configured limit")
    void retrieve_manyCandidates_respectsConfiguredLimit() {
        var product = buildProduct();
        product.setAiSummary("Balanced and sweet.");

        var reviews = List.of(
                review("Sweet and balanced", 4, 10),
                review("Nutty aroma", 4, 9),
                review("Low acidity", 5, 8),
                review("Creamy body", 5, 7)
        );

        var result = retriever.retrieve(product, reviews, "Is it sweet?");

        assertThat(result).hasSizeLessThanOrEqualTo(3);
    }

    private ProductInfo buildProduct() {
        var product = new ProductInfo();
        product.setId(UUID.randomUUID());
        product.setName("Brazilian Espresso Beans");
        product.setDescription("A rich medium roast with chocolate, caramel, and hazelnut notes.");
        product.setBrandName("Iced Latte Roasters");
        product.setSellerName("Iced Latte");
        product.setOriginCountry("Brazil");
        product.setAverageRating(new BigDecimal("4.7"));
        product.setReviewsCount(24);
        return product;
    }

    private ProductReview review(String text, int rating, int likes) {
        var review = new ProductReview();
        review.setText(text);
        review.setProductRating(rating);
        review.setLikesCount(likes);
        review.setCreatedAt(OffsetDateTime.now());
        return review;
    }
}
