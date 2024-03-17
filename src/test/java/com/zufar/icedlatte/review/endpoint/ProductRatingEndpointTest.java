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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBodySchemaResponse;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Testcontainers
@DisplayName("ProductRatingEndpoint Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductRatingEndpointTest {


    private static final String RATING_RESPONSE_SCHEMA = "rating/model/schema/rating-response-schema.json";
    private static final String AFFOGATO_ID = "ba5f15c4-1f72-4b97-b9cf-4437e5c6c2fa";
    private static final String ESPRESSO_ID = "ad0ef2b7-816b-4a11-b361-dfcbe705fc96";
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
                .basePath(ProductRatingEndpoint.RATING_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should add rating successfully and return object containing rating and product id")
    void shouldAddRatingSuccessfully() {
        Response response = given(specification)
                .post("/{productId}/ratings/{rating}", AFFOGATO_ID, 3);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, RATING_RESPONSE_SCHEMA)
                .body("rating", equalTo(3));
    }

    @Test
    @DisplayName("Should fetch average rating successfully and return object containing avg rating and product id")
    void shouldFetchAverageRatingSuccessfully() {
        // No authorization is required
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductRatingEndpoint.RATING_URL)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .get("/{productId}/ratings", ESPRESSO_ID);

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, RATING_RESPONSE_SCHEMA)
                .body("rating", equalTo(4.0F));
    }

    @Test
    @DisplayName("For method POST (add review) access to rating URL w/o token is forbidden. Should return 400 Bad Request")
    void shouldReturnBadRequestOnPostWOToken() {
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductRatingEndpoint.RATING_URL)
                .accept(ContentType.JSON);

        Response response = given(specification)
                .post("/{productId}/ratings/{rating}", AFFOGATO_ID, 1);
        response.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}