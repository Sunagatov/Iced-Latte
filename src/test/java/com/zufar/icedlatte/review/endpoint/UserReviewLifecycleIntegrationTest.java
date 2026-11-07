package com.zufar.icedlatte.review.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;

@DisplayName("User review lifecycle integration tests")
class UserReviewLifecycleIntegrationTest extends AuthenticatedUserIntegrationSupport {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void deletingAnAccountRefreshesItsReviewsAndVotesOnOtherReviews() {
        AuthenticatedUser author = registerAndAuthenticateUser();
        AuthenticatedUser other = registerAndAuthenticateUser();
        createReview(author);
        String otherReview = createReview(other);
        given(authenticatedJsonSpec(ProductReviewsEndpoint.PRODUCT_REVIEWS_URL, author.accessToken()))
                .body("{\"isLike\":true}")
                .post("/{productId}/reviews/{reviewId}/likes", PRODUCT_ID, otherReview)
                .then()
                .statusCode(200)
                .body("likesCount", equalTo(1));
        given(authenticatedJsonSpec(USER_REVIEWS_BASE_PATH, author.accessToken()))
                .delete()
                .then()
                .statusCode(200);
        given(authenticatedJsonSpec(ProductReviewsEndpoint.PRODUCT_REVIEWS_URL, other.accessToken()))
                .get("/{productId}/review", PRODUCT_ID)
                .then()
                .statusCode(200)
                .body("likesCount", equalTo(0));
        var product = java.util.UUID.fromString(PRODUCT_ID);
        Long actualCount =
                jdbc.queryForObject("SELECT count(*) FROM product_reviews WHERE product_id=?", Long.class, product);
        Long storedCount = jdbc.queryForObject("SELECT reviews_count FROM product WHERE id=?", Long.class, product);
        Double actualAverage = jdbc.queryForObject(
                "SELECT avg(rating) FROM product_reviews WHERE product_id=?", Double.class, product);
        Double storedAverage =
                jdbc.queryForObject("SELECT average_rating FROM product WHERE id=?", Double.class, product);
        org.assertj.core.api.Assertions.assertThat(storedCount).isEqualTo(actualCount);
        org.assertj.core.api.Assertions.assertThat(storedAverage)
                .isCloseTo(actualAverage, org.assertj.core.data.Offset.offset(0.051));
    }

    private static final String USER_REVIEWS_BASE_PATH = "/api/v1/users";
    private static final String PRODUCT_ID = "d1a2b3c4-0001-4000-8000-000000000007";
    private static final String REVIEW_TEXT = "Fresh review from integration test user";
    private static final int REVIEW_RATING = 5;

    @Test
    @DisplayName("Should return empty user reviews for a fresh user")
    void shouldReturnEmptyUserReviewsForFreshUser() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(USER_REVIEWS_BASE_PATH, user.accessToken()))
                .get("/reviews")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("reviewsWithRatings", hasSize(0))
                .body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName("Should create review and expose it in current user reviews")
    void shouldCreateReviewAndExposeItInCurrentUserReviews() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        String reviewId = createReview(user);

        given(authenticatedJsonSpec(USER_REVIEWS_BASE_PATH, user.accessToken()))
                .get("/reviews")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("reviewsWithRatings", hasSize(1))
                .body("reviewsWithRatings[0].productReviewId", equalTo(reviewId))
                .body("reviewsWithRatings[0].productId", equalTo(PRODUCT_ID))
                .body("reviewsWithRatings[0].text", equalTo(REVIEW_TEXT))
                .body("reviewsWithRatings[0].productRating", equalTo(REVIEW_RATING))
                .body("totalElements", equalTo(1));
    }

    @Test
    @DisplayName("Should delete review and remove it from current user reviews")
    void shouldDeleteReviewAndRemoveItFromCurrentUserReviews() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        String reviewId = createReview(user);

        given(authenticatedJsonSpec(ProductReviewsEndpoint.PRODUCT_REVIEWS_URL, user.accessToken()))
                .delete("/{productId}/reviews/{reviewId}", PRODUCT_ID, reviewId)
                .then()
                .statusCode(HttpStatus.OK.value());

        given(authenticatedJsonSpec(USER_REVIEWS_BASE_PATH, user.accessToken()))
                .get("/reviews")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("reviewsWithRatings", hasSize(0))
                .body("totalElements", equalTo(0));
    }

    private String createReview(AuthenticatedUser user) {
        return given(authenticatedJsonSpec(ProductReviewsEndpoint.PRODUCT_REVIEWS_URL, user.accessToken()))
                .body("""
                        {
                          "text": "%s",
                          "rating": %d
                        }
                        """.formatted(REVIEW_TEXT, REVIEW_RATING))
                .post("/{productId}/reviews", PRODUCT_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getString("productReviewId");
    }
}
