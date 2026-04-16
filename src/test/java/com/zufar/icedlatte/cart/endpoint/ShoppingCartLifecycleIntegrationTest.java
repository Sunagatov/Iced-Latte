package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Shopping cart lifecycle integration tests")
class ShoppingCartLifecycleIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";
    private static final String SECOND_PRODUCT_ID = "ad0ef2b7-816b-4a11-b361-dfcbe705fc96";

    @Test
    @DisplayName("Should create an empty cart for a fresh user")
    void shouldCreateEmptyCartForFreshUser() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("userId", notNullValue())
                .body("items", hasSize(0))
                .body("itemsQuantity", equalTo(0))
                .body("productsQuantity", equalTo(0));
    }

    @Test
    @DisplayName("Should merge quantity when the same product is added twice")
    void shouldMergeQuantityWhenSameProductIsAddedTwice() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        addItem(user, PRODUCT_ID, 1)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].productInfo.id", equalTo(PRODUCT_ID))
                .body("items[0].productQuantity", equalTo(1))
                .body("itemsQuantity", equalTo(1))
                .body("productsQuantity", equalTo(1));

        addItem(user, PRODUCT_ID, 2)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].productInfo.id", equalTo(PRODUCT_ID))
                .body("items[0].productQuantity", equalTo(3))
                .body("itemsQuantity", equalTo(1))
                .body("productsQuantity", equalTo(3));
    }

    @Test
    @DisplayName("Should keep carts isolated between different users")
    void shouldKeepCartsIsolatedBetweenDifferentUsers() {
        AuthenticatedUser firstUser = registerAndAuthenticateUser();
        AuthenticatedUser secondUser = registerAndAuthenticateUser();

        addItem(firstUser, PRODUCT_ID, 2)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].productInfo.id", equalTo(PRODUCT_ID));

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, secondUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(0))
                .body("itemsQuantity", equalTo(0))
                .body("productsQuantity", equalTo(0));

        addItem(secondUser, SECOND_PRODUCT_ID, 1)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].productInfo.id", equalTo(SECOND_PRODUCT_ID));

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, firstUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].productInfo.id", equalTo(PRODUCT_ID))
                .body("items[0].productQuantity", equalTo(2));

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, secondUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].productInfo.id", equalTo(SECOND_PRODUCT_ID))
                .body("items[0].productQuantity", equalTo(1));
    }

    @Test
    @DisplayName("Should return 404 when user tries to update another user's cart item")
    void shouldReturn404WhenUserTriesToUpdateAnotherUsersCartItem() {
        AuthenticatedUser firstUser = registerAndAuthenticateUser();
        AuthenticatedUser secondUser = registerAndAuthenticateUser();

        Response firstResponse = addItem(firstUser, PRODUCT_ID, 1);
        String foreignCartItemId = firstResponse.then()
                .extract()
                .jsonPath()
                .getString("items[0].id");

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, secondUser.accessToken()))
                .body("""
                        {
                          "shoppingCartItemId": "%s",
                          "productQuantityChange": 1
                        }
                        """.formatted(foreignCartItemId))
                .patch("/items")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private Response addItem(AuthenticatedUser user, String productId, int quantity) {
        return given(authenticatedJsonSpec(CartEndpoint.CART_URL, user.accessToken()))
                .body("""
                        {
                          "items": [
                            {
                              "productId": "%s",
                              "productQuantity": %d
                            }
                          ]
                        }
                        """.formatted(productId, quantity))
                .post("/items");
    }
}