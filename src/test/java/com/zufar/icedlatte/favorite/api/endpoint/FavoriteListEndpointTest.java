package com.zufar.icedlatte.favorite.api.endpoint;

import com.zufar.icedlatte.favorite.endpoint.FavoritesEndpoint;
import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import static com.zufar.icedlatte.test.config.RestAssertion.*;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;

import org.springframework.boot.test.web.server.LocalServerPort;

@DisplayName("FavoriteListEndpointTest Tests")
class FavoriteListEndpointTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    private static final String FAVORITE_LIST_SCHEMA = "favorite/model/schema/favorite-list-schema.json";
    private static final String FAVORITE_LIST_ERROR_SCHEMA = "favorite/model/schema/favorite-list-error-schema.json";
    private static final String PRODUCT_ADD_TO_FAVORITE_LIST = "/favorite/model/product-add-to-favorite-list.json";
    private static final String PRODUCT_NOT_EXIST_ADD_TO_FAVORITE_LIST = "/favorite/model/product-not-exist-add-to-favorite-list.json";

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
                .basePath(FavoritesEndpoint.FAVORITES_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should add product to FavoriteList")
    void shouldAddProductToFavoriteList() {
        assertRestApiOkResponse(given(specification).body(getRequestBody(PRODUCT_ADD_TO_FAVORITE_LIST)).post(), FAVORITE_LIST_SCHEMA);
    }

    @Test
    @DisplayName("Should add not exist product to FavoriteList")
    void shouldAddNotExistProductToFavoriteList() {
        assertRestApiBodySchemaResponse(
                given(specification).body(getRequestBody(PRODUCT_NOT_EXIST_ADD_TO_FAVORITE_LIST)).post(),
                HttpStatus.BAD_REQUEST, FAVORITE_LIST_ERROR_SCHEMA);
    }

    @Test
    @DisplayName("Should delete product from FavoriteList")
    void shouldDeleteProductFromFavoriteList() {
        assertRestApiEmptyBodyResponse(
                given(specification).delete("/{productId}", "418499f3-d951-40bf-9414-5cb90ab21ecb"),
                HttpStatus.OK);
    }

    @Test
    @DisplayName("Should delete not exist product from FavoriteList")
    void shouldDeleteNotExistProductFromFavoriteList() {
        assertRestApiBodySchemaResponse(
                given(specification).delete("/{productId}", "456123456"),
                HttpStatus.BAD_REQUEST, FAVORITE_LIST_ERROR_SCHEMA);
    }

    @Test
    @DisplayName("Should get FavoriteList")
    void shouldGetFavoriteList() {
        assertRestApiOkResponse(given(specification).get(), FAVORITE_LIST_SCHEMA);
    }
}
