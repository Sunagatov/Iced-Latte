package com.zufar.onlinestore.product.endpoint.e2e;

import com.zufar.onlinestore.product.endpoint.ProductsEndpoint;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseProductEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;
    static RequestSpecification specification;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    @BeforeEach
    void baseSetUp() {
        specification = given().log().all(true)
                .baseUri("http://localhost:" + port + ProductsEndpoint.PRODUCTS_URL)
                .accept(ContentType.JSON);
    }

    public static ValidatableResponse checkStatusCodeInResponse(String url, Map<String, ?> params, int code, String schema) {
        return given(specification)
                .queryParams(params).get(url).then()
                .statusCode(code)
                .body(matchesJsonSchemaInClasspath(schema))
                .time(lessThan(1500L));
    }

    public static void checkStatusCodeInResponse(String url, Map<String, ?> params, int code) {
        given(specification)
                .queryParams(params)
                .get(url)
                .then()
                .statusCode(code)
                .body(emptyOrNullString())
                .time(lessThan(1500L));
    }

    public void baseNegativeCheck(String posUrl, String schema) {
        checkStatusCodeInResponse(posUrl, Collections.emptyMap(), HttpStatus.NOT_FOUND.value(), schema);
    }

    public void basePositiveHasItemsCheck(String path, String item, String posUrl, String schema, Map<String, ?> params) {
        checkStatusCodeInResponse(posUrl, params, HttpStatus.OK.value(), schema)
                .body(path, hasItems(item));
    }
}
