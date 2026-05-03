package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.cart.endpoint.CartEndpoint;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@DisplayName("Order history integration tests")
class OrderHistoryIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String ORDERS_URL = OrderEndpoint.ORDERS_URL;
    private static final String CART_URL = CartEndpoint.CART_URL;
    private static final String PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";

    @Test
    @DisplayName("Pagination returns correct page metadata")
    void paginationWorks() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        createOrders(user, 3);

        given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .queryParam("page", 0)
                .queryParam("size", 2)
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(2))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(2))
                .body("page", equalTo(0))
                .body("size", equalTo(2));

        given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .queryParam("page", 1)
                .queryParam("size", 2)
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(1))
                .body("page", equalTo(1));
    }

    @Test
    @DisplayName("Status filter returns only matching orders")
    void statusFilterWorks() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        createOrders(user, 2);

        // Cancel the first order
        String orderId = given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .queryParam("page", 0)
                .queryParam("size", 1)
                .get()
                .then()
                .extract()
                .jsonPath()
                .getString("content[0].id");

        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId + "/cancel", user.accessToken()))
                .post()
                .then()
                .statusCode(HttpStatus.OK.value());

        // Filter by CREATED — should have 1
        given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .queryParam("status", "CREATED")
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("totalElements", equalTo(1))
                .body("content[0].status", equalTo("CREATED"));

        // Filter by CANCELLED — should have 1
        given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .queryParam("status", "CANCELLED")
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("totalElements", equalTo(1))
                .body("content[0].status", equalTo("CANCELLED"));
    }

    @Test
    @DisplayName("Empty result for user with no orders")
    void emptyHistoryForNewUser() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(0))
                .body("totalElements", equalTo(0));
    }

    private void createOrders(AuthenticatedUser user, int count) {
        for (int i = 0; i < count; i++) {
            given(authenticatedJsonSpec(CART_URL + "/items", user.accessToken()))
                    .body("""
                            {"items": [{"productId": "%s", "productQuantity": 1}]}
                            """.formatted(PRODUCT_ID))
                    .post()
                    .then()
                    .statusCode(HttpStatus.OK.value());

            given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                    .body("""
                            {
                              "recipientName": "User",
                              "recipientSurname": "Test",
                              "address": {"country":"UK","city":"London","line":"1 St","postcode":"E1 1AA"}
                            }
                            """)
                    .post()
                    .then()
                    .statusCode(HttpStatus.CREATED.value());
        }
    }
}
