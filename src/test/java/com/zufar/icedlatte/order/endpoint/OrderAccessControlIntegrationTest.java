package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.cart.endpoint.CartEndpoint;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Order access control integration tests")
class OrderAccessControlIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String ORDERS_URL = OrderEndpoint.ORDERS_URL;
    private static final String CART_URL = CartEndpoint.CART_URL;
    private static final String PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";

    @Test
    @DisplayName("User cannot view another user's order (403)")
    void cannotViewOtherUsersOrder() {
        AuthenticatedUser userA = registerAndAuthenticateUser();
        String orderId = createOrderForUser(userA);

        AuthenticatedUser userB = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId, userB.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("User cannot cancel another user's order (403)")
    void cannotCancelOtherUsersOrder() {
        AuthenticatedUser userA = registerAndAuthenticateUser();
        String orderId = createOrderForUser(userA);

        AuthenticatedUser userB = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId + "/cancel", userB.accessToken()))
                .post()
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("User cannot refund another user's order (403)")
    void cannotRefundOtherUsersOrder() {
        AuthenticatedUser userA = registerAndAuthenticateUser();
        String orderId = createOrderForUser(userA);

        AuthenticatedUser userB = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId + "/refund", userB.accessToken()))
                .body("{}")
                .post()
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("User cannot reorder another user's order (403)")
    void cannotReorderOtherUsersOrder() {
        AuthenticatedUser userA = registerAndAuthenticateUser();
        String orderId = createOrderForUser(userA);

        AuthenticatedUser userB = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId + "/reorder", userB.accessToken()))
                .post()
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedReturns401() {
        given(jsonSpec(ORDERS_URL))
                .get()
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Nonexistent order returns 404")
    void nonexistentOrderReturns404() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(ORDERS_URL + "/00000000-0000-0000-0000-000000000000", user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    private String createOrderForUser(AuthenticatedUser user) {
        given(authenticatedJsonSpec(CART_URL + "/items", user.accessToken()))
                .body("""
                        {"items": [{"productId": "%s", "productQuantity": 1}]}
                        """.formatted(PRODUCT_ID))
                .post()
                .then()
                .statusCode(HttpStatus.OK.value());

        return given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .body("""
                        {
                          "recipientName": "Owner",
                          "recipientSurname": "User",
                          "address": {"country":"UK","city":"London","line":"1 St","postcode":"E1 1AA"}
                        }
                        """)
                .post()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id");
    }
}
