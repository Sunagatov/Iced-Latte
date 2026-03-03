package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.springframework.boot.test.web.server.LocalServerPort;

@DisplayName("ShoppingCartEndpointTest Tests")
class ShoppingCartEndpointTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    protected static RequestSpecification specification;

    private static final String SHOPPING_CART_SCHEMA_LOCATION = "cart/model/schema/cart-schema.json";
    private static final String SHOPPING_CART_ADD_BODY_LOCATION = "/cart/model/cart-add-body.json";
    private static final String SHOPPING_CART_UPDATE_BODY_LOCATION = "/cart/model/cart-update-body.json";
    private static final String SHOPPING_CART_UPDATE_BAD_BODY_LOCATION = "/cart/model/cart-update-bad-body.json";
    private static final String SHOPPING_CART_DELETE_BODY_LOCATION = "/cart/model/cart-delete-body.json";

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
                .basePath(CartEndpoint.CART_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should retrieve shopping cart successfully")
    void shouldRetrieveShoppingCartSuccessfully() {
        assertRestApiOkResponse(given(specification).get(), SHOPPING_CART_SCHEMA_LOCATION);
        given(specification).get().then()
                .body("id", notNullValue())
                .body("userId", notNullValue())
                .body("items", notNullValue())
                .body("itemsQuantity", greaterThanOrEqualTo(0))
                .body("productsQuantity", greaterThanOrEqualTo(0))
                .body("itemsTotalPrice", greaterThanOrEqualTo(0.0f));
    }

    @Test
    @DisplayName("Should add item to shopping cart successfully")
    void shouldAddItemToShoppingCart() {
        Response response = given(specification).body(getRequestBody(SHOPPING_CART_ADD_BODY_LOCATION)).post("/items");
        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
        response.then()
                .body("items", hasSize(greaterThan(0)))
                .body("itemsQuantity", greaterThan(0))
                .body("productsQuantity", greaterThan(0));
    }

    @Test
    @DisplayName("Should update shopping cart item quantity successfully")
    void shouldUpdateItemQuantityInShoppingCart() {
        Response response = given(specification).body(getRequestBody(SHOPPING_CART_UPDATE_BODY_LOCATION)).patch("/items");
        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
        response.then()
                .body("items", notNullValue())
                .body("itemsQuantity", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent shopping cart item")
    void shouldReturnNotFoundWhenUpdatingNonExistentItem() {
        given(specification).body(getRequestBody(SHOPPING_CART_UPDATE_BAD_BODY_LOCATION)).patch("/items")
                .then().statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should delete items from shopping cart successfully")
    void shouldDeleteItemsFromShoppingCart() {
        Response response = given(specification).body(getRequestBody(SHOPPING_CART_DELETE_BODY_LOCATION)).delete("/items");
        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
        response.then()
                .body("items", notNullValue())
                .body("itemsQuantity", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should return 401 when accessing cart without authentication")
    void shouldReturnUnauthorizedWithoutToken() {
        given().log().all(true).port(port).basePath(CartEndpoint.CART_URL)
                .contentType(ContentType.JSON).accept(ContentType.JSON)
                .get()
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should return 400 for invalid request body")
    void shouldReturnBadRequestForInvalidBody() {
        given(specification).body("{\"invalid\": \"data\"}").post("/items")
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
