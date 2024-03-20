package com.zufar.icedlatte.review.endpoint;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBadRequestResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBodySchemaResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiNotFoundResponse;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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
    private static final String REVIEW_WITH_RATING_RESPONSE_SCHEMA = "review/model/schema/review-with-rating-response-schema.json";
    private static final String FAILED_REVIEW_SCHEMA = "common/model/schema/failed-request-schema.json";
    private static final String EXPECTED_REVIEW = "Wow, Iced Latte is so good!!!";
    private static final String AMERICANO_ID = "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5";
    private static final String AFFOGATO_ID = "ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa";
    private static final String START_OF_REVIEW_FOR_AMERICANO = "Review for Americano";

    protected static RequestSpecification specification;
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

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
    @DisplayName("Should add review successfully and return object containing review text")
    void shouldAddReviewSuccessfully() {
        String body = getRequestBody(REVIEW_ADD_BODY);

        Response response = given(specification)
                .body(body)
                .post("/{productId}/reviews", AFFOGATO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, REVIEW_RESPONSE_SCHEMA)
                .body("text", equalTo(EXPECTED_REVIEW));

        removeReview(AFFOGATO_ID, response);
    }

    @Test
    @DisplayName("Should fetch reviews and ratings with default pagination and sorting")
    void shouldFetchReviewsAndRatingsWithDefaultPaginationAndSorting() {
        // No authorization is required
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .get("/{productId}/reviews", AMERICANO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, REVIEWS_WITH_RATINGS_RESPONSE_SCHEMA)
                .body("totalElements", equalTo(4))
                .body("totalPages", equalTo(1))
                .body("reviewsWithRatings[0].rating", equalTo(4))
                .body("reviewsWithRatings[0].reviewText", equalTo(null))
                .body("reviewsWithRatings[0].userName", equalTo("Emma"))
                .body("reviewsWithRatings[1].rating", equalTo(5))
                .body("reviewsWithRatings[1].reviewText", startsWith(START_OF_REVIEW_FOR_AMERICANO))
                .body("reviewsWithRatings[1].userName", equalTo("John"))
                .body("reviewsWithRatings[2].rating", equalTo(null))
                .body("reviewsWithRatings[2].reviewText", startsWith(START_OF_REVIEW_FOR_AMERICANO))
                .body("reviewsWithRatings[2].userName", equalTo("Jane"))
                .body("reviewsWithRatings[3].rating", equalTo(null))
                .body("reviewsWithRatings[3].reviewText", startsWith(START_OF_REVIEW_FOR_AMERICANO))
                .body("reviewsWithRatings[3].userName", equalTo("Michael"));
    }

    @Test
    @DisplayName("Should fetch review and rating successfully for an authorized user")
    void shouldFetchReviewAndRatingSuccessfully() {
        // no review and no rating
        Response responseForNoReviewAndRating = given(specification)
                .get("/{productId}/review", AFFOGATO_ID);

        assertRestApiBodySchemaResponse(responseForNoReviewAndRating, HttpStatus.OK, REVIEW_WITH_RATING_RESPONSE_SCHEMA)
                .body("reviewText", nullValue())
                .body("rating", nullValue());

        // Add at least review
        String body = getRequestBody(REVIEW_ADD_BODY);
        Response responseWithReviewId = given(specification)
                .body(body)
                .post("/{productId}/reviews", AFFOGATO_ID);

        Response responseWithReview = given(specification)
                .get("/{productId}/review", AFFOGATO_ID);
        assertRestApiBodySchemaResponse(responseWithReview, HttpStatus.OK, REVIEW_WITH_RATING_RESPONSE_SCHEMA)
                .body("reviewText", equalTo(EXPECTED_REVIEW))
                .body("rating", nullValue());

        removeReview(AFFOGATO_ID, responseWithReviewId);
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
    @DisplayName("For methods POST, DELETE and GET /exists access to review URL w/o token is forbidden. Should return 400 Bad Request")
    void shouldReturnBadRequestOnPostDeleteGetExistsWOToken() {
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductReviewEndpoint.PRODUCT_REVIEW_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        String body = getRequestBody(REVIEW_ADD_BODY);
        Response responsePost = given(specification)
                .body(body)
                .post("/{productId}/reviews", AMERICANO_ID);
        Response responseDelete = given(specification)
                .delete("/{productId}/reviews/{reviewId}", AMERICANO_ID, UUID.randomUUID());

        Response responseGetExists = given(specification)
                .get("/{productId}/review", AMERICANO_ID);

        responsePost.then().statusCode(HttpStatus.BAD_REQUEST.value());
        responseDelete.then().statusCode(HttpStatus.BAD_REQUEST.value());
        responseGetExists.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}