package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.cart.endpoint.CartEndpoint;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Order lifecycle integration tests")
class OrderLifecycleIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String ORDERS_URL = OrderEndpoint.ORDERS_URL;
    private static final String CART_URL = CartEndpoint.CART_URL;
    private static final String PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";

    @Test
    @DisplayName("Full lifecycle: add to cart → create order → get order → cancel")
    void fullOrderLifecycle() {
        AuthenticatedUser user = registerAndAuthenticateUser();

        // 1. Add item to cart
        given(authenticatedJsonSpec(CART_URL + "/items", user.accessToken()))
                .body("""
                        {"items": [{"productId": "%s", "productQuantity": 2}]}
                        """.formatted(PRODUCT_ID))
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1));

        // 2. Create order with inline address
        String orderId = given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .body("""
                        {
                          "recipientName": "John",
                          "recipientSurname": "Doe",
                          "recipientPhone": "+44123456789",
                          "address": {
                            "country": "UK",
                            "city": "London",
                            "line": "221B Baker Street",
                            "postcode": "NW1 6XE"
                          }
                        }
                        """)
                .post()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", notNullValue())
                .body("status", equalTo("CREATED"))
                .body("itemsQuantity", equalTo(1))
                .body("items", hasSize(1))
                .body("items[0].productsQuantity", equalTo(2))
                .extract()
                .jsonPath()
                .getString("id");

        // 3. Cart should be cleared
        given(authenticatedJsonSpec(CART_URL, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(0));

        // 4. Get order by ID
        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(orderId))
                .body("status", equalTo("CREATED"))
                .body("recipientName", equalTo("John"))
                .body("canCancel", equalTo(true))
                .body("canRefund", equalTo(false))
                .body("cancellationDeadline", notNullValue());

        // 5. Cancel the order
        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId + "/cancel", user.accessToken()))
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("CANCELLED"));

        // 6. Verify order is now CANCELLED
        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId, user.accessToken()))
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("CANCELLED"))
                .body("canCancel", equalTo(false));

        // 7. Cannot cancel again
        given(authenticatedJsonSpec(ORDERS_URL + "/" + orderId + "/cancel", user.accessToken()))
                .post()
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Order appears in paginated history after creation")
    void orderAppearsInHistory() {
        AuthenticatedUser user = registerAndAuthenticateUser();
        addItemAndCreateOrder(user);

        given(authenticatedJsonSpec(ORDERS_URL, user.accessToken()))
                .queryParam("page", 0)
                .queryParam("size", 10)
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(greaterThan(0)))
                .body("totalElements", greaterThan(0))
                .body("page", equalTo(0));
    }

    private void addItemAndCreateOrder(AuthenticatedUser user) {
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
                          "recipientName": "Test",
                          "recipientSurname": "User",
                          "address": {"country":"UK","city":"London","line":"1 Test St","postcode":"E1 1AA"}
                        }
                        """)
                .post()
                .then()
                .statusCode(HttpStatus.CREATED.value());
    }
}
