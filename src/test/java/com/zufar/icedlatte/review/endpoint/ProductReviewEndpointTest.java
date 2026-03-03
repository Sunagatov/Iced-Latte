package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static com.zufar.icedlatte.test.config.RestAssertion.*;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.springframework.boot.test.web.server.LocalServerPort;

@DisplayName("ProductReviewEndpoint Tests")
class ProductReviewEndpointTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    private static final String REVIEW_ADD_BODY = "/review/model/add-review-body.json";
    private static final String REVIEW_ADD_BAD_BODY = "/review/model/add-review-bad-body.json";
    private static final String REVIEW_ADD_EMPTY_TEXT = "/review/model/add-review-empty-text.json";
    private static final String REVIEW_RESPONSE_SCHEMA = "review/model/schema/review-response-schema.json";
    private static final String REVIEWS_WITH_RATINGS_RESPONSE_SCHEMA = "review/model/schema/review-response-schema.json";
    private static final String FAILED_REVIEW_SCHEMA = "common/model/schema/failed-request-schema.json";
    private static final String EXPECTED_REVIEW = "Wow, Iced Latte is so good!!!";
    private static final String AMERICANO_ID = "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5";
    private static final String AFFOGATO_ID = "ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa";
    private static final String START_OF_REVIEW_FOR_AMERICANO = "Review for Americano";
    private static final String RATING_RESPONSE_SCHEMA = "review/model/schema/stats-response-schema.json";
    private static final String ESPRESSO_ID = "ad0ef2b7-816b-4a11-b361-dfcbe705fc96";
    private static final String LIKE_REQUEST_BODY = "{\"isLike\": true}";
    private static final String DISLIKE_REQUEST_BODY = "{\"isLike\": false}";
    private static final int EXPECTED_PRODUCT_RATING = 3;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    protected static RequestSpecification specification;

    private static String cachedJwtToken;

    @BeforeEach
    void tokenAndSpecification() {
        if (cachedJwtToken == null) {
            cachedJwtToken = getJwtToken(port, email, password);
        }
        specification = given()
                .log().all(true)
                .port(port)
                .header("Authorization", "Bearer " + cachedJwtToken)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    void removeReview(final Response response) {
        var currentReviewId = response.getBody().path("productReviewId");
        if (currentReviewId != null) {
            given(specification).delete("/{productId}/reviews/{reviewId}", AFFOGATO_ID, currentReviewId);
        }
    }

    @Test
    @DisplayName("Should add review successfully and return object containing review")
    void shouldAddReviewSuccessfully() {
        Response response = given(specification).body(getRequestBody(REVIEW_ADD_BODY))
                .post("/{productId}/reviews", AFFOGATO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", equalTo(EXPECTED_REVIEW))
                .body("productRating", equalTo(EXPECTED_PRODUCT_RATING))
                .body("productReviewId", notNullValue());

        removeReview(response);
    }

    @Test
    @DisplayName("Should like and dislike review successfully")
    void shouldLikeAndDislikeReviewSuccessfully() {
        String reviewId = "38939e11-cf1f-42ff-bfbd-ba1c2bc867f8";

        assertRestApiBodySchemaResponse(
                given(specification).body(LIKE_REQUEST_BODY).post("/{productId}/reviews/{reviewId}/likes", ESPRESSO_ID, reviewId),
                HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("productReviewId", notNullValue())
                .body("productId", equalTo(ESPRESSO_ID));

        assertRestApiBodySchemaResponse(
                given(specification).body(DISLIKE_REQUEST_BODY).post("/{productId}/reviews/{reviewId}/likes", ESPRESSO_ID, reviewId),
                HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("productReviewId", notNullValue())
                .body("productId", equalTo(ESPRESSO_ID));
    }

    @Test
    @DisplayName("Should fetch review successfully for an authorized user")
    void shouldFetchReviewSuccessfully() {
        assertRestApiBodySchemaResponse(given(specification).get("/{productId}/review", AFFOGATO_ID), HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", nullValue())
                .body("productRating", nullValue());

        assertRestApiBodySchemaResponse(given(specification).get("/{productId}/review", AMERICANO_ID), HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", startsWith(START_OF_REVIEW_FOR_AMERICANO))
                .body("productRating", equalTo(EXPECTED_PRODUCT_RATING));
    }

    @Test
    @DisplayName("Should fetch review statistics successfully")
    void shouldFetchReviewStatsSuccessfully() {
        RequestSpecification noAuth = given().log().all(true).port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL).accept(ContentType.JSON);

        assertRestApiBodySchemaResponse(given(noAuth).get("/{productId}/reviews/statistics", ESPRESSO_ID), HttpStatus.OK, RATING_RESPONSE_SCHEMA)
                .body("avgRating", equalTo(3.0f))
                .body("reviewsCount", equalTo(1))
                .body("productId", notNullValue())
                .body("ratingMap.star1", equalTo(0))
                .body("ratingMap.star2", equalTo(0))
                .body("ratingMap.star3", equalTo(1))
                .body("ratingMap.star4", equalTo(0))
                .body("ratingMap.star5", equalTo(0));
    }

    @Test
    @DisplayName("Reviews and ratings with default pagination and sorting for unauthorized user. Should return 200 OK")
    void shouldSuccessfullyReturnReviewsForDefaultPaginationAndSortingForAnonymous() {
        RequestSpecification noAuth = given().log().all(true).port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL).contentType(ContentType.JSON).accept(ContentType.JSON);

        assertRestApiOkResponse(given(noAuth).get("/{productId}/reviews", AMERICANO_ID), REVIEWS_WITH_RATINGS_RESPONSE_SCHEMA);
    }

    @Test
    @DisplayName("Should return 404 Not Found on attempt to add review for invalid product ID")
    void shouldReturnNotFoundOnAttemptToAddNonExistentProduct() {
        assertRestApiNotFoundResponse(
                given(specification).body(getRequestBody(REVIEW_ADD_BODY)).post("/{productId}/reviews", UUID.randomUUID()),
                FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Missing required fields in add review request body. Should return 400 Bad Request")
    void shouldReturnBadRequestForBadBody() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(REVIEW_ADD_BAD_BODY)).post("/{productId}/reviews", AMERICANO_ID),
                FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Review text is an empty string in add review request body. Should return 400 Bad Request")
    void shouldReturnBadRequestForEmptyReviewText() {
        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(REVIEW_ADD_EMPTY_TEXT)).post("/{productId}/reviews", AMERICANO_ID),
                FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Should return 400 Bad Request on attempt to create review if previous still exists")
    void shouldReturnBadRequestOnAttemptToCreateReviewIfPreviousIsNotRemoved() {
        given(specification).body(getRequestBody(REVIEW_ADD_BODY)).post("/{productId}/reviews", AMERICANO_ID);

        assertRestApiBadRequestResponse(
                given(specification).body(getRequestBody(REVIEW_ADD_BODY)).post("/{productId}/reviews", AMERICANO_ID),
                FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Should delete existing review successfully")
    void shouldDeleteExistingReviewSuccessfully() {
        Response responsePost = given(specification).body(getRequestBody(REVIEW_ADD_BODY))
                .post("/{productId}/reviews", AFFOGATO_ID);
        responsePost.then().statusCode(HttpStatus.OK.value());

        given(specification)
                .delete("/{productId}/reviews/{productReviewId}", AFFOGATO_ID, responsePost.then().extract().path("productReviewId").toString())
                .then().statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Should return 404 Not Found on attempt to delete non-existent review")
    void shouldReturnNotFoundOnDeleteNonExistentReview() {
        assertRestApiNotFoundResponse(
                given(specification).delete("/{productId}/reviews/{productReviewId}", AMERICANO_ID, UUID.randomUUID()),
                FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for protected endpoints without token")
    void shouldReturnUnauthorizedWithoutToken() {
        RequestSpecification noAuth = given().log().all(true).port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL).contentType(ContentType.JSON).accept(ContentType.JSON);

        given(noAuth).body(getRequestBody(REVIEW_ADD_BODY)).post("/{productId}/reviews", AMERICANO_ID)
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
        given(noAuth).delete("/{productId}/reviews/{reviewId}", AMERICANO_ID, UUID.randomUUID())
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
        given(noAuth).get("/{productId}/review", AMERICANO_ID)
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}