package com.zufar.icedlatte.cart.endpoint;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import org.springframework.http.HttpStatus;


@Testcontainers
@DisplayName("ShoppingCartEndpointTest Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShoppingCartEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    protected static RequestSpecification specification;
    private static final String SHOPPING_CART_SCHEMA_LOCATION = "cart/model/schema/cart-schema.json";
    private static final String SHOPPING_CART_ADD_BODY_LOCATION = "/cart/model/cart-add-body.json";
    private static final String SHOPPING_CART_UPDATE_BODY_LOCATION = "/cart/model/cart-update-body.json";
    private static final String SHOPPING_CART_UPDATE_BAD_BODY_LOCATION = "/cart/model/cart-update-bad-body.json";
    private static final String SHOPPING_CART_DELETE_BODY_LOCATION = "/cart/model/cart-delete-body.json";

    @BeforeEach
    void tokenAndSpecification() {
        var jwtToken = getJwtToken(port, email, password);
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
        response.then()
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
        String body = getRequestBody(SHOPPING_CART_ADD_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .post("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
        response.then()
                .body("items", hasSize(greaterThan(0)))
                .body("itemsQuantity", greaterThan(0))
                .body("productsQuantity", greaterThan(0));
    }

    @Test
    @DisplayName("Should update shopping cart item quantity successfully")
    void shouldUpdateItemQuantityInShoppingCart() {
        String body = getRequestBody(SHOPPING_CART_UPDATE_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .patch("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
        response.then()
                .body("items", notNullValue())
                .body("itemsQuantity", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent shopping cart item")
    void shouldReturnNotFoundWhenUpdatingNonExistentItem() {
        String body = getRequestBody(SHOPPING_CART_UPDATE_BAD_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .patch("/items");

        response.then().statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Should delete items from shopping cart successfully")
    void shouldDeleteItemsFromShoppingCart() {
        String body = getRequestBody(SHOPPING_CART_DELETE_BODY_LOCATION);

        Response response = given(specification)
                .body(body)
                .delete("/items");

        assertRestApiOkResponse(response, SHOPPING_CART_SCHEMA_LOCATION);
        response.then()
                .body("items", notNullValue())
                .body("itemsQuantity", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should return 401 when accessing cart without authentication")
    void shouldReturnUnauthorizedWithoutToken() {
        RequestSpecification unauthenticatedSpec = given()
                .log().all(true)
                .port(port)
                .basePath(CartEndpoint.CART_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        Response response = given(unauthenticatedSpec)
                .get();

        response.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should return 400 for invalid request body")
    void shouldReturnBadRequestForInvalidBody() {
        String invalidBody = "{\"invalid\": \"data\"}";

        Response response = given(specification)
                .body(invalidBody)
                .post("/items");

        response.then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}