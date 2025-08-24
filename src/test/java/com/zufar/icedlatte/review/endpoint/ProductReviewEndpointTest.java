package com.zufar.icedlatte.review.endpoint;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBadRequestResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBodySchemaResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiNotFoundResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@Testcontainers
@DisplayName("ProductReviewEndpoint Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductReviewEndpointTest {

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

    protected static RequestSpecification specification;
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    @BeforeEach
    void tokenAndSpecification() {
        var jwtToken = getJwtToken(port, email, password);
        specification = given()
                .log().all(true)
                .port(port)
                .header("Authorization", "Bearer " + jwtToken)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    void removeReview(final String currentProductId, final Response response) {
        var currentReviewId = response.getBody().path("productReviewId");
        // clean up the DB
        if (currentReviewId != null && currentProductId != null) {
            given(specification)
                    .delete("/{productId}/reviews/{reviewId}", currentProductId, currentReviewId);
        }
    }

    @Test
    @DisplayName("Should add review successfully and return object containing review")
    void shouldAddReviewSuccessfully() {
        String body = getRequestBody(REVIEW_ADD_BODY);

        Response response = given(specification)
                .body(body)
                .post("/{productId}/reviews", AFFOGATO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", equalTo(EXPECTED_REVIEW))
                .body("productRating", equalTo(EXPECTED_PRODUCT_RATING))
                .body("productReviewId", notNullValue());

        removeReview(AFFOGATO_ID, response);
    }

    @Test
    @DisplayName("Should like and dislike review successfully")
    void shouldLikeAndDislikeReviewSuccessfully() {
        String reviewId = "38939e11-cf1f-42ff-bfbd-ba1c2bc867f8";
        
        // Test like
        Response likeResponse = given(specification)
                .body(LIKE_REQUEST_BODY)
                .post("/{productId}/reviews/{reviewId}/likes", ESPRESSO_ID, reviewId);

        assertRestApiBodySchemaResponse(likeResponse, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("productReviewId", notNullValue())
                .body("productId", equalTo(ESPRESSO_ID));
        
        // Test dislike
        Response dislikeResponse = given(specification)
                .body(DISLIKE_REQUEST_BODY)
                .post("/{productId}/reviews/{reviewId}/likes", ESPRESSO_ID, reviewId);

        assertRestApiBodySchemaResponse(dislikeResponse, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("productReviewId", notNullValue())
                .body("productId", equalTo(ESPRESSO_ID));
    }

    @Test
    @DisplayName("Should fetch review successfully for an authorized user")
    void shouldFetchReviewSuccessfully() {
        // no review was created by Jane Smith
        Response response = given(specification)
                .get("/{productId}/review", AFFOGATO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", nullValue())
                .body("productRating", nullValue());

        // review exists
        response = given(specification)
                .get("/{productId}/review", AMERICANO_ID);
        assertRestApiBodySchemaResponse(response, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", startsWith(START_OF_REVIEW_FOR_AMERICANO))
                .body("productRating", equalTo(EXPECTED_PRODUCT_RATING)); // import static com.example.Constants.EXPECTED_PRODUCT_RATING;
    }

    @Test
    @DisplayName("Should fetch review statistics successfully")
    void shouldFetchReviewStatsSuccessfully() {
        // No authorization is required
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .get("/{productId}/reviews/statistics", ESPRESSO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, RATING_RESPONSE_SCHEMA)
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
        // No authorization is required
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .get("/{productId}/reviews", AMERICANO_ID);

        assertRestApiOkResponse(response, REVIEWS_WITH_RATINGS_RESPONSE_SCHEMA);
    }

    @Test
    @DisplayName("Should return 404 Not Found on attempt to add review for invalid product ID")
    void shouldReturnNotFoundOnAttemptToAddNonExistentProduct() {
        String body = getRequestBody(REVIEW_ADD_BODY);

        var randomProduct = UUID.randomUUID();

        Response response = given(specification)
                .body(body)
                .post("/{productId}/reviews", randomProduct);

        assertRestApiNotFoundResponse(response, FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Missing required fields in add review request body. Should return 400 Bad Request")
    void shouldReturnBadRequestForBadBody() {
        String body = getRequestBody(REVIEW_ADD_BAD_BODY);

        Response response = given(specification)
                .body(body)
                .post("/{productId}/reviews", AMERICANO_ID);

        assertRestApiBadRequestResponse(response, FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Review text is an empty string in add review request body. Should return 400 Bad Request")
    void shouldReturnBadRequestForEmptyReviewText() {
        String body = getRequestBody(REVIEW_ADD_EMPTY_TEXT);

        Response response = given(specification)
                .body(body)
                .post("/{productId}/reviews", AMERICANO_ID);

        assertRestApiBadRequestResponse(response, FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Should return 400 Bad Request on attempt to create review if previous still exists")
    void shouldReturnBadRequestOnAttemptToCreateReviewIfPreviousIsNotRemoved() {
        String body = getRequestBody(REVIEW_ADD_BODY);

        // 1st create review - ok
        given(specification)
                .body(body)
                .post("/{productId}/reviews", AMERICANO_ID);

        // 2nd create review - no way, remove previous first
        Response response = given(specification)
                .body(body)
                .post("/{productId}/reviews", AMERICANO_ID);

        assertRestApiBadRequestResponse(response, FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Should delete existing review successfully")
    void shouldDeleteExistingReviewSuccessfully() {
        String body = getRequestBody(REVIEW_ADD_BODY);

        Response responsePost = given(specification)
                .body(body)
                .post("/{productId}/reviews", AFFOGATO_ID);

        responsePost.then().statusCode(HttpStatus.OK.value());
        var reviewId = responsePost.then().extract().path("productReviewId").toString();

        Response response = given(specification)
                .delete("/{productId}/reviews/{productReviewId}", AFFOGATO_ID, reviewId);

        response.then().statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Should return 404 Not Found on attempt to delete non-existent review")
    void shouldReturnNotFoundOnDeleteNonExistentReview() {
        Response response = given(specification)
                .delete("/{productId}/reviews/{productReviewId}", AMERICANO_ID, UUID.randomUUID());

        assertRestApiNotFoundResponse(response, FAILED_REVIEW_SCHEMA);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for protected endpoints without token")
    void shouldReturnUnauthorizedWithoutToken() {
        RequestSpecification unauthenticatedSpec = given()
                .log().all(true)
                .port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        String body = getRequestBody(REVIEW_ADD_BODY);
        
        Response responsePost = given(unauthenticatedSpec)
                .body(body)
                .post("/{productId}/reviews", AMERICANO_ID);
        
        Response responseDelete = given(unauthenticatedSpec)
                .delete("/{productId}/reviews/{reviewId}", AMERICANO_ID, UUID.randomUUID());

        Response responseGetReview = given(unauthenticatedSpec)
                .get("/{productId}/review", AMERICANO_ID);

        responsePost.then().statusCode(HttpStatus.UNAUTHORIZED.value());
        responseDelete.then().statusCode(HttpStatus.UNAUTHORIZED.value());
        responseGetReview.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}