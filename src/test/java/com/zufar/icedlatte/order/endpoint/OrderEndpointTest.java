package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.openapi.dto.OrderStatus;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBadRequestResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiBodySchemaResponse;
import static com.zufar.icedlatte.test.config.RestAssertion.assertRestApiOkResponse;
import static com.zufar.icedlatte.test.config.RestUtils.getJwtToken;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Testcontainers
@DisplayName("OrderEndpoint Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderEndpointTest {

    private static final String ORDER_ADD_BODY_1 = "/order/model/add-order-body-1.json";
    private static final String ORDER_ADD_BODY_2 = "/order/model/add-order-body-1.json";
    private static final String ORDER_ADD_BAD_BODY = "/order/model/add-order-bad-body.json";
    private static final String ORDER_RESPONSE_SCHEMA = "order/model/schema/order-response-schema.json";
    private static final String FAILED_ORDER_SCHEMA = "order/model/schema/failed-order-schema.json";
    private static final String ORDER_LIST_SCHEMA = "order/model/schema/order-list-schema.json";

    protected static RequestSpecification specification;
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    @Value("${jwt.email}")
    protected String email;

    @Value("${jwt.password}")
    protected String password;

    @BeforeEach
    void tokenAndSpecification() {
        var jwtToken = getJwtToken(port, email, password);
        specification = given()
                .log().all(true)
                .port(port)
                .header("Authorization", "Bearer " + jwtToken)
                .basePath(OrderEndpoint.ORDER_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should add order successfully and return object containing status 'CREATED'")
    void shouldAddOrderSuccessfully() {
        String body = getRequestBody(ORDER_ADD_BODY_1);

        Response response = given(specification)
                .body(body)
                .post();

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, ORDER_RESPONSE_SCHEMA)
                .body("status", equalTo(OrderStatus.CREATED.toString()));
    }

    @Test
    @DisplayName("Missing required fields in request body. Should return 400 Bad Request")
    void shouldReturnBadRequestForBadBody() {
        String body = getRequestBody(ORDER_ADD_BAD_BODY);

        Response response = given(specification)
                .body(body)
                .post();

        assertRestApiBadRequestResponse(response, FAILED_ORDER_SCHEMA);
    }

    @Test
    @DisplayName("Can't access order URL w/o token. Should return 401 Unauthorized")
    void shouldReturnUnauthorized() {
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(OrderEndpoint.ORDER_URL)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);

        String body = getRequestBody(ORDER_ADD_BODY_1);
        Response responsePost = given(specification)
                .body(body)
                .post();
        Response responseGet = given(specification)
                .get();

        responsePost.then().statusCode(HttpStatus.UNAUTHORIZED.value());
        responseGet.then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should return list of orders")
    void shouldReturnListOfOrders() {
        // Create two orders
        given(specification)
                .body(getRequestBody(ORDER_ADD_BODY_1))
                .post();
        given(specification)
                .body(getRequestBody(ORDER_ADD_BODY_2))
                .post();

        Response responseNoParam = given(specification)
                .get();
        Response responseWithParam = given(specification)
                .param("status", OrderStatus.CREATED)
                .get();

        var CREATED = OrderStatus.CREATED.toString();

        assertRestApiOkResponse(responseNoParam, ORDER_LIST_SCHEMA);
        responseNoParam.then().body("[0].status", is(CREATED));
        responseNoParam.then().body("[1].status", is(CREATED));

        assertRestApiOkResponse(responseWithParam, ORDER_LIST_SCHEMA);
        responseWithParam.then().body("[0].status", is(CREATED));
        responseWithParam.then().body("[1].status", is(CREATED));
    }

    @Test
    @DisplayName("Should return empty list of orders")
    void shouldReturnEmptyListOfOrders() {
        // Create two orders
        given(specification)
                .body(getRequestBody(ORDER_ADD_BODY_1))
                .post();
        given(specification)
                .body(getRequestBody(ORDER_ADD_BODY_2))
                .post();

        Response response = given(specification)
                .param("status", OrderStatus.DELIVERY)
                .get();

        assertRestApiBodySchemaResponse(response, HttpStatus.OK, ORDER_LIST_SCHEMA)
                .body("$[]", Matchers.hasSize(0));
    }

    @Test
    @DisplayName("Incorrect value for parameter status. Should return 400 Bad Request")
    void shouldReturnBadRequestForBadStatusParam() {
        Response response = given(specification)
                .param("status", "WRONG_VALUE")
                .get();

        assertRestApiBadRequestResponse(response, FAILED_ORDER_SCHEMA);
        response.then().body("message", is("Incorrect status value. Supported status: [CREATED, DELIVERY, FINISHED]"));
    }
}