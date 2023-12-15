package com.zufar.icedlatte.favorite.api.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.zufar.icedlatte.favorite.endpoint.FavoritesEndpoint;
import com.zufar.icedlatte.security.endpoint.UserSecurityEndpoint;
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

import java.io.IOException;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBodySchemaResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiEmptyBodyResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static io.restassured.RestAssured.given;

@Testcontainers
@DisplayName("FavoriteListEndpointTest Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FavoriteListEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.secret}")
    protected String secretKey;

    @Value("${jwt.expiration}")
    protected Long expiration;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    private static final String AUTHENTICATE_TEMPLATE = "/security/model/authenticate-template.json";

    private static final String FAVORITE_LIST_SCHEMA = "favorite/model/schema/favorite-list-schema.json";
    private static final String FAVORITE_LIST_ERROR_SCHEMA = "favorite/model/schema/favorite-list-error-schema.json";
    private static final String PRODUCT_ADD_TO_FAVORITE_LIST = "/favorite/model/product-add-to-favorite-list.json";
    private static final String PRODUCT_NOT_EXIST_ADD_TO_FAVORITE_LIST = "/favorite/model/product-not-exist-add-to-favorite-list.json";

    protected static RequestSpecification specification;

    @BeforeEach
    void tokenAndSpecification() {
        String jwtToken = getJwtToken();
        specification = given()
                .log().all(true)
                .port(port)
                .header("Authorization", "Bearer " + jwtToken)
                .basePath(FavoritesEndpoint.FAVORITES_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    protected String getRequestBody(String resourcePath) {
        try {
            JsonNode json = JsonLoader.fromResource(resourcePath);
            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getJwtToken() {
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(UserSecurityEndpoint.USER_SECURITY_API_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        String body = getRequestBody(AUTHENTICATE_TEMPLATE)
                .formatted(email, password);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        String jwtToken = response.getBody().path("token");

        if (isJwtTokenNotValid(jwtToken)) {
            throw new IllegalArgumentException("JWT Token is empty or null. Test failed.");
        }

        return jwtToken;
    }

    protected boolean isJwtTokenNotValid(String jwtToken) {
        return jwtToken == null || jwtToken.isEmpty();
    }

    @Test
    @DisplayName("Should add product to FavoriteList")
    void shouldAddProductToFavoriteList() {
        String body = getRequestBody(PRODUCT_ADD_TO_FAVORITE_LIST);

        Response response = given(specification)
                .body(body)
                .post();

        assertRestApiOkResponse(response, FAVORITE_LIST_SCHEMA);
    }

    @Test
    @DisplayName("Should add not exist product to FavoriteList")
    void shouldAddNotExistProductToFavoriteList() {
        String body = getRequestBody(PRODUCT_NOT_EXIST_ADD_TO_FAVORITE_LIST);

        Response response = given(specification)
                .body(body)
                .post();

        assertRestApiBodySchemaResponse(response, HttpStatus.BAD_REQUEST, FAVORITE_LIST_ERROR_SCHEMA);
    }

    @Test
    @DisplayName("Should delete product from FavoriteList")
    void shouldDeleteProductFromFavoriteList() {
        String productId = "418499f3-d951-40bf-9414-5cb90ab21ecb";

        Response response = given(specification)
                .delete("/{productId}", productId);

        assertRestApiEmptyBodyResponse(response, HttpStatus.OK);
    }

    @Test
    @DisplayName("Should delete not exist product from FavoriteList")
    void shouldDeleteNotExistProductFromFavoriteList() {
        String productId = "456123456";

        Response response = given(specification)
                .delete("/{productId}", productId);

        assertRestApiBodySchemaResponse(response, HttpStatus.BAD_REQUEST, FAVORITE_LIST_ERROR_SCHEMA);
    }

    @Test
    @DisplayName("Should get FavoriteList")
    void shouldGetFavoriteList() {

        Response response = given(specification)
                .get();

        assertRestApiOkResponse(response, FAVORITE_LIST_SCHEMA);
    }
}
