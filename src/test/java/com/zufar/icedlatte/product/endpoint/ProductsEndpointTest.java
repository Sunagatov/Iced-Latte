package com.zufar.icedlatte.product.endpoint;

import com.zufar.icedlatte.product.util.PaginationAndSortingAttribute;
import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.zufar.icedlatte.test.config.RestAssertion.*;
import static com.zufar.icedlatte.test.config.RestUtils.getRequestBody;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.springframework.boot.test.web.server.LocalServerPort;

@DisplayName("ProductsEndpoint Tests")
class ProductsEndpointTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    private static final String PRODUCT_FAILED_SCHEMA_LOCATION = "product/model/schema/product-failed-schema.json";
    private static final String PRODUCT_LIST_PAGINATION_SCHEMA_LOCATION = "product/model/schema/product-list-pagination-schema.json";
    private static final String PRODUCT_LIST_BY_ID_SCHEMA_LOCATION = "product/model/schema/product-list-by-id-schema.json";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String EXPECTED_PRODUCT_NAME = "Nitro Coffee";
    private static final String PRODUCTS_PATH_TO_NAME = "products.name";
    private static final String INVALID_SORT_ATTRIBUTE = "name354864904";
    private static final String VALID_PRODUCT_ID = "418499f3-d951-40bf-9414-5cb90ab21ecb";

    protected static RequestSpecification specification;

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
        given(specification).get("/{productId}", VALID_PRODUCT_ID)
                .then()
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
        given(specification).get("/{productId}", UUID.randomUUID().toString())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should retrieve products successfully by IDs")
    void shouldRetrieveProductsSuccessfullyByIds() {
        given(specification)
                .contentType(ContentType.JSON)
                .body(getRequestBody("/product/model/productsByIdsRequest1.json"))
                .post("/ids")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(greaterThan(0)))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue());
    }

    @Test
    @DisplayName("Should retrieve products successfully by IDs and contain 'Nitro Coffee'")
    void shouldRetrieveProductsSuccessfullyByIdsAndContainSpecificProduct() {
        assertRestApiOkResponse(
                given(specification).contentType(ContentType.JSON)
                        .body(getRequestBody("/product/model/productsByIdsRequest1.json"))
                        .post("/ids"),
                PRODUCT_LIST_BY_ID_SCHEMA_LOCATION, "name", EXPECTED_PRODUCT_NAME);
    }

    @Test
    @DisplayName("Should return not found for invalid product IDs")
    void shouldReturnNotFoundForInvalidProductIds() {
        assertRestApiNotFoundResponse(
                given(specification).contentType(ContentType.JSON)
                        .body(getRequestBody("/product/model/productsByIdsRequest2.json"))
                        .post("ids"),
                PRODUCT_FAILED_SCHEMA_LOCATION);
    }

    @Test
    @DisplayName("Should fetch products with pagination and sorting")
    void shouldFetchProductsWithPaginationAndSorting() {
        Map<String, Object> params = new HashMap<>();
        params.put(PaginationAndSortingAttribute.PAGE, 1);
        params.put(PaginationAndSortingAttribute.SIZE, 1);
        params.put(PaginationAndSortingAttribute.SORT_ATTRIBUTE, NAME_ATTRIBUTE);
        params.put(PaginationAndSortingAttribute.SORT_DIRECTION, Sort.Direction.DESC.name().toLowerCase(Locale.ROOT));

        given(specification).queryParams(params).get()
                .then()
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

        given(specification).queryParams(params).get()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", notNullValue());
    }

    @Test
    @DisplayName("Should contain product with name 'Nitro Coffee'")
    void shouldContainProductWithNameNitroCoffee() {
        Map<String, Object> params = new HashMap<>();
        params.put(PaginationAndSortingAttribute.PAGE, 9);
        params.put(PaginationAndSortingAttribute.SIZE, 1);
        params.put(PaginationAndSortingAttribute.SORT_ATTRIBUTE, NAME_ATTRIBUTE);
        params.put(PaginationAndSortingAttribute.SORT_DIRECTION, Sort.Direction.DESC.name().toLowerCase(Locale.ROOT));

        assertRestApiOkResponse(
                given(specification).queryParams(params).get(),
                PRODUCT_LIST_PAGINATION_SCHEMA_LOCATION, PRODUCTS_PATH_TO_NAME, EXPECTED_PRODUCT_NAME);
    }
}
