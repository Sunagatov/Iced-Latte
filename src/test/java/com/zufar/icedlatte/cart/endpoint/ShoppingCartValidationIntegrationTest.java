package com.zufar.icedlatte.cart.endpoint;

import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@DisplayName("Shopping cart validation integration tests")
class ShoppingCartValidationIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";

    @Test
    @DisplayName("Should return bad request when adding empty item list")
    void shouldReturnBadRequestWhenAddingEmptyItemList() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, user.accessToken()))
                .body("""
                        {
                          "items": []
                        }
                        """)
                .post("/items")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should return bad request when deleting empty item id list")
    void shouldReturnBadRequestWhenDeletingEmptyItemIdList() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, user.accessToken()))
                .body("""
                        {
                          "shoppingCartItemIds": []
                        }
                        """)
                .delete("/items")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should return bad request when quantity change is zero")
    void shouldReturnBadRequestWhenQuantityChangeIsZero() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        Response response = addItem(user);
        String shoppingCartItemId = response.then()
                .extract()
                .jsonPath()
                .getString("items[0].id");

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, user.accessToken()))
                .body("""
                        {
                          "shoppingCartItemId": "%s",
                          "productQuantityChange": 0
                        }
                        """.formatted(shoppingCartItemId))
                .patch("/items")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should not delete another user's cart item ids")
    void shouldNotDeleteAnotherUsersCartItemIds() {
        AuthenticatedUser firstUser = registerAndAuthenticateUser();
        AuthenticatedUser secondUser = registerAndAuthenticateUser();

        Response firstResponse = addItem(firstUser);
        String foreignItemId = firstResponse.then()
                .extract()
                .jsonPath()
                .getString("items[0].id");

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, secondUser.accessToken()))
                .body("""
                        {
                          "shoppingCartItemIds": ["%s"]
                        }
                        """.formatted(foreignItemId))
                .delete("/items")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(0))
                .body("itemsQuantity", equalTo(0))
                .body("productsQuantity", equalTo(0));

        given(authenticatedJsonSpec(CartEndpoint.CART_URL, firstUser.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(foreignItemId))
                .body("items[0].productInfo.id", equalTo(PRODUCT_ID))
                .body("items[0].productQuantity", equalTo(1));
    }

    private Response addItem(AuthenticatedUser user) {
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
                        """.formatted(ShoppingCartValidationIntegrationTest.PRODUCT_ID, 1))
                .post("/items");
    }
}