package com.zufar.icedlatte.product.endpoint;

import com.zufar.icedlatte.product.util.PaginationAndSortingAttribute;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.zufar.icedlatte.test.config.RestAssertion.*;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.springframework.http.HttpStatus;


@Testcontainers
@DisplayName("ProductsEndpoint Tests")
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductsEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    protected static RequestSpecification specification;

    private static final String PRODUCT_SCHEMA_LOCATION = "product/model/schema/product-schema.json";
    private static final String PRODUCT_FAILED_SCHEMA_LOCATION = "product/model/schema/product-failed-schema.json";
    private static final String PRODUCT_LIST_PAGINATION_SCHEMA_LOCATION = "product/model/schema/product-list-pagination-schema.json";
    private static final String PRODUCT_LIST_BY_ID_SCHEMA_LOCATION = "product/model/schema/product-list-by-id-schema.json";

    private static final String NAME_ATTRIBUTE = "name";
    private static final String EXPECTED_PRODUCT_NAME = "Nitro Coffee";
    private static final String PRODUCTS_PATH_TO_NAME = "products.name";
    private static final String INVALID_SORT_ATTRIBUTE = "name354864904";
    private static final String VALID_PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";

    @BeforeEach
    void setEndpointUrl() {
        specification = given()
                .log().all(true)
                .port(port)
                .basePath(ProductsEndpoint.PRODUCTS_URL)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should retrieve product successfully by ID")
    void shouldRetrieveProductSuccessfullyById() {
        Response response = given(specification)
                .get("/{productId}", VALID_PRODUCT_ID);

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("name", notNullValue())
                .body("price", greaterThan(0.0f))
                .body("quantity", greaterThanOrEqualTo(0))
                .body("active", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for invalid product ID")
    void shouldReturn404ForInvalidProductId() {
        String invalidProductId = UUID.randomUUID().toString();

        Response response = given(specification)
                .get("/{productId}", invalidProductId);

        response.then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should retrieve products successfully by IDs")
    void shouldRetrieveProductsSuccessfullyByIds() {
        String requestBody = getRequestBody("/product/model/productsByIdsRequest1.json");

        Response response = given(specification)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/ids");

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(greaterThan(0)))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue());
    }

    @Test
    @DisplayName("Should retrieve products successfully by IDs and contain 'Nitro Coffee'")
    void shouldRetrieveProductsSuccessfullyByIdsAndContainSpecificProduct() {
        String requestBody = getRequestBody("/product/model/productsByIdsRequest1.json");

        Response response = given(specification)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/ids");

        assertRestApiOkResponse(
                response,
                PRODUCT_LIST_BY_ID_SCHEMA_LOCATION,
                "name",
                EXPECTED_PRODUCT_NAME
        );
    }

    @Test
    @DisplayName("Should return not found for invalid product IDs")
    void shouldReturnNotFoundForInvalidProductIds() {
        String requestBody = getRequestBody("/product/model/productsByIdsRequest2.json");

        Response response = given(specification)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("ids");

        assertRestApiNotFoundResponse(response, PRODUCT_FAILED_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should fetch products with pagination and sorting")
    void shouldFetchProductsWithPaginationAndSorting() {
        Map<String, Object> params = new HashMap<>();
        params.put(PaginationAndSortingAttribute.PAGE, 1);
        params.put(PaginationAndSortingAttribute.SIZE, 1);
        params.put(PaginationAndSortingAttribute.SORT_ATTRIBUTE, NAME_ATTRIBUTE);
        params.put(PaginationAndSortingAttribute.SORT_DIRECTION, Sort.Direction.DESC.name().toLowerCase(Locale.ROOT));

        Response response = given(specification)
                .queryParams(params)
                .get();

        response.then()
                .statusCode(HttpStatus.OK.value())
                .body("products", hasSize(1))
                .body("page", equalTo(1))
                .body("size", equalTo(1))
                .body("totalElements", greaterThan(0))
                .body("totalPages", greaterThan(0));
    }

    @Test
    @DisplayName("Should return 400 for invalid sort attribute")
    void shouldReturn400ForInvalidSortAttribute() {
        Map<String, Object> params = new HashMap<>();
        params.put(PaginationAndSortingAttribute.PAGE, 15);
        params.put(PaginationAndSortingAttribute.SIZE, 8);
        params.put(PaginationAndSortingAttribute.SORT_ATTRIBUTE, INVALID_SORT_ATTRIBUTE);
        params.put(PaginationAndSortingAttribute.SORT_DIRECTION, Sort.Direction.DESC.name().toLowerCase(Locale.ROOT));

        Response response = given(specification)
                .queryParams(params)
                .get();

        response.then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should contain product with name 'Nitro Coffee'")
    void shouldContainProductWithNameNitroCoffee() {
        Map<String, Object> params = new HashMap<>();
        params.put(PaginationAndSortingAttribute.PAGE, 5);
        params.put(PaginationAndSortingAttribute.SIZE, 1);
        params.put(PaginationAndSortingAttribute.SORT_ATTRIBUTE, NAME_ATTRIBUTE);
        params.put(PaginationAndSortingAttribute.SORT_DIRECTION, Sort.Direction.DESC.name().toLowerCase(Locale.ROOT));

        Response response = given(specification)
                .queryParams(params)
                .get();

        assertRestApiOkResponse(
                response,
                PRODUCT_LIST_PAGINATION_SCHEMA_LOCATION,
                PRODUCTS_PATH_TO_NAME,
                EXPECTED_PRODUCT_NAME
        );
    }
}
