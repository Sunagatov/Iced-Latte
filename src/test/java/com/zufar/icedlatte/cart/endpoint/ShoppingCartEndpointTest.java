package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.test.config.AbstractE2ETest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiNotFoundResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static io.restassured.RestAssured.given;


@Testcontainers
@DisplayName("ShoppingCartEndpointTest Tests")
class ShoppingCartEndpointTest extends AbstractE2ETest {

    private static final String SHOPPING_CART_SCHEMA_LOCATION = "cart/model/schema/cart-schema.json";
    private static final String SHOPPING_CART_ADD_BODY_LOCATION = "/cart/model/cart-add-body.json";
    private static final String SHOPPING_CART_UPDATE_BODY_LOCATION = "/cart/model/cart-update-body.json";
    private static final String SHOPPING_CART_UPDATE_BAD_BODY_LOCATION = "/cart/model/cart-update-bad-body.json";
    private static final String SHOPPING_CART_DELETE_BODY_LOCATION = "/cart/model/cart-delete-body.json";

    @BeforeEach
    void tokenAndSpecification() {
        getJwtToken();
        specification = given()
                .log().all(true)
                .port(port)
                .header("Authorization", "Bearer " + jwtToken)
                .basePath(CartEndpoint.CART_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should retrieve shopping cart successfully")
    void shouldRetrieveShoppingCartSuccessfully() {

        Response response = given(specification)
                .get();
        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should add shopping cart successfully")
    void shouldAddItemFromShoppingCart() {

        String body = getRequestBody(SHOPPING_CART_ADD_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .post("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should update shopping cart successfully")
    void shouldUpdateItemFromShoppingCart() {

        String body = getRequestBody(SHOPPING_CART_UPDATE_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .patch("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should update not found shopping cart successfully")
    void shouldUpdateNotFoundShoppingCart() {

        String body = getRequestBody(SHOPPING_CART_UPDATE_BAD_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .patch("/items");

        assertRestApiNotFoundResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should delete shopping cart successfully")
    void shouldDeleteItemFromShoppingCart() {

        String body = getRequestBody(SHOPPING_CART_DELETE_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .delete("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }
}