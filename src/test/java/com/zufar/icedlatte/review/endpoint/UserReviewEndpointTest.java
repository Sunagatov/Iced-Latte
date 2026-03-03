package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("UserReviewEndpoint Tests")
class UserReviewEndpointTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    private static String cachedJwtToken;
    private RequestSpecification specification;

    @BeforeEach
    void setUp() {
        if (cachedJwtToken == null) {
            cachedJwtToken = getJwtToken(port, email, password);
        }
        specification = given()
                .log().all(true)
                .port(port)
                .header("Authorization", "Bearer " + cachedJwtToken)
                .basePath("/api/v1/users")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("GET /api/v1/users/reviews returns 200 with pagination wrapper")
    void getUserReviews_authenticated_returns200() {
        given(specification)
                .get("/reviews")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("reviewsWithRatings", notNullValue());
    }

    @Test
    @DisplayName("GET /api/v1/users/reviews with explicit pagination params returns 200")
    void getUserReviews_withPaginationParams_returns200() {
        given(specification)
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sort_attribute", "createdAt")
                .queryParam("sort_direction", "desc")
                .get("/reviews")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("GET /api/v1/users/reviews without token returns 401")
    void getUserReviews_unauthenticated_returns401() {
        given()
                .port(port)
                .accept(ContentType.JSON)
                .get("/api/v1/users/reviews")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
