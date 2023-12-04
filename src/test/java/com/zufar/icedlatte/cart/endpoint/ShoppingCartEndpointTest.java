package com.zufar.icedlatte.cart.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.zufar.icedlatte.security.endpoint.UserSecurityEndpoint;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiNotFoundResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static io.restassured.RestAssured.given;


@Testcontainers
@DisplayName("ShoppingCartEndpointTest Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShoppingCartEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.secret}")
    protected String secretKey;

    @Value("${jwt.expiration}")
    protected Long expiration;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;
    protected static String jwtToken = "";

    private static final String AUTHENTICATE_TEMPLATE = "/security/model/authenticate-template.json";

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    protected static RequestSpecification specification;
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

    protected String getRequestBody(String resourcePath) {
        try {
            JsonNode json = JsonLoader.fromResource(resourcePath);
            return json.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void getJwtToken(){
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(UserSecurityEndpoint.USER_SECURITY_API_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        String body = getRequestBody(AUTHENTICATE_TEMPLATE)
                .formatted(email, password);

        Response response = given(specification)
                .body(body)
                .post("/authenticate");

        jwtToken = response.getBody().path("token");
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