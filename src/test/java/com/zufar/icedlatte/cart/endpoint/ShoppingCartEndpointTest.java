package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.test.config.AbstractE2ETest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static io.restassured.RestAssured.given;


@Testcontainers
@Disabled("Disabled as while the issue 'ContainerFetch Can't get Docker image: RemoteDockerImag...' was not fixed.")
@DisplayName("ShoppingCartEndpointTest Tests")
class ShoppingCartEndpointTest extends AbstractE2ETest {

    private static final String SHOPPING_CART_SCHEMA_LOCATION = "cart/model/schema/cart-schema.json";

    @BeforeAll
    static void generate(){
        generateJwtToken();
    }

    @BeforeEach
    void specification() {
        //String jwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzAxMDc3NzkwLCJleHAiOjI3MDEwNzc3OTB9.y0JN0nTlObmrG9cql9m6n9oJJCx-AvppQVwyvhRxNzk";
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

        String body = """
                {
                  "items": [
                    {
                      "productId": "9ed30979-1da4-40c2-87e3-5c498ea070ab",
                      "productQuantity": 1
                    }
                  ]
                }""";

        Response response = given(specification)
                .body(body)
                .post("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should update shopping cart successfully")
    void shouldUpdateItemFromShoppingCart() {

        String body = """
                {
                  "shoppingCartItemId": "9ed30979-1da4-40c2-87e3-5c498ea070ab",
                  "productQuantityChange": 4
                }""";

        Response response = given(specification)
                .body(body)
                .patch("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should delete shopping cart successfully")
    void shouldDeleteItemFromShoppingCart() {

        String body = """
                {
                  "shoppingCartItemIds": [
                    "9ed30979-1da4-40c2-87e3-5c498ea070ab"
                  ]
                }""";

        Response response = given(specification)
                .body(body)
                .delete("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
    }
}