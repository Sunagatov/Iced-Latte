package com.zufar.onlinestore.product.endpoint.e2e;

import com.zufar.onlinestore.product.endpoint.ProductsEndpoint;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThan;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductsEndpointTest {

    private static final String BASE_URL = "http://localhost:";
    private static final String PRODUCT_URL = "/api/v1/products";
    private static final String PRODUCT_SCHEMA = "product/model/schema/product-schema.json";
    private static final String PRODUCT_FAILED_SCHEMA = "product/model/schema/product-failed-schema.json";
    private static final String PRODUCT_LIST_PAGINATION_SCHEMA = "product/model/schema/product-list-pagination-schema.json";
    private static final long MAX_RESPONSE_TIME = 1500L;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    static DockerImageName MOCKSERVER_IMAGE;
    static MockServerContainer mockServer;
    static RequestSpecification specification;
    static MockServerClient mockServerClient;

    @LocalServerPort
    protected Integer port;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mockServerClient.reset();
        specification = given().log().all(true)
                .baseUri(BASE_URL + port + PRODUCT_URL)
                .accept(ContentType.JSON);
    }

    @BeforeAll
    static void baseSetUp() {
        MOCKSERVER_IMAGE = DockerImageName.parse("mockserver/mockserver")
                .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
        mockServer = new MockServerContainer(MOCKSERVER_IMAGE);
        mockServer.start();
    }

    @AfterEach
    void resetMockServer() {
        mockServerClient.reset();
    }

    @AfterAll
    static void tearDown() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Value("classpath:product/model/product-base-model.json")
    private Resource productResponseResource;

    @Value("classpath:product/model/product-error-model.json")
    private Resource errorResponseResource;

    @Value("classpath:product/model/product-pagination-americano-model.json")
    private Resource paginationAmericanoResponseResource;

    @Value("classpath:product/model/product-pagination-macchiato-model.json")
    private Resource paginationMacchiatoResponseResource;

    @Test
    @DisplayName("Get Product Details with Valid ID Returns 200 OK")
    void shouldReturnProductDetailsWhenProductIdIsValid() {
        String productId = "418499f3-d951-40bf-9414-5cb90ab21ecb";
        String mockResponse = loadProductJsonResource(productResponseResource);

        mockServerClient
                .when(request().withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL + "/" + productId))
                .respond(response().withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse));

        checkHttpStatusCodeInResponse("/" + productId, Collections.emptyMap(), HttpStatus.OK.value(), PRODUCT_SCHEMA);
    }

    @Test
    @DisplayName("Get Product Details with Invalid ID Returns 404 Not Found")
    void shouldReturnNotFoundWhenProductIdIsInvalid() {
        String invalidProductId = "f1a1d5c1-a6f7-4e2b-8cfd-3d3f52e1abcd";
        String mockErrorResponse = loadProductJsonResource(errorResponseResource);

        mockServerClient
                .when(request().withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL + "/" + invalidProductId))
                .respond(response().withStatusCode(HttpStatus.NOT_FOUND.value())
                        .withBody(mockErrorResponse));

        baseNegativeCheck("/" + invalidProductId, PRODUCT_FAILED_SCHEMA);
    }

    @Test
    @DisplayName("Get All Products with Default Pagination Returns 200 OK")
    void shouldReturnAllProductsWithDefaultPagination() {
        String mockResponse = loadProductJsonResource(paginationMacchiatoResponseResource);

        mockServerClient
                .when(request().withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL)
                        .withQueryStringParameters(
                                param("page", "1"),
                                param("size", "1"),
                                param("sort_attribute", "name"),
                                param("sort_direction", "desc")))
                .respond(response().withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse));

        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 1);
        params.put("sort_attribute", "name");
        params.put("sort_direction", "desc");

        checkHttpStatusCodeInResponse("", params, HttpStatus.OK.value(), PRODUCT_LIST_PAGINATION_SCHEMA);
    }

    @Test
    @DisplayName("Unknown Query Parameter Returns 400 Bad Request")
    void shouldReturnBadRequestForUnknownQueryParameter() {
        mockServerClient
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL)
                        .withQueryStringParameters(
                                param("page", "15"),
                                param("size", "8"),
                                param("sort_attribute", "names"),
                                param("sort_direction", "desc")
                        ))
                .respond(response().withStatusCode(HttpStatus.FORBIDDEN.value()));

        Map<String, Object> params = new HashMap<>();
        params.put("page", 15);
        params.put("size", 8);
        params.put("sort_attribute", "names");
        params.put("sort_direction", "desc");

        checkHttpStatusCodeInResponse("", params, HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldContainProductWithNameAmericano() {
        String expectedProductName = "Americano";
        String mockResponse = loadProductJsonResource(paginationAmericanoResponseResource);

        mockServerClient
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL)
                        .withQueryStringParameter("page", "5")
                        .withQueryStringParameter("size", "1")
                        .withQueryStringParameter("sort_attribute", "name")
                        .withQueryStringParameter("sort_direction", "desc"))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse));

        Map<String, Object> params = new HashMap<>();
        params.put("page", 5);
        params.put("size", 1);
        params.put("sort_attribute", "name");
        params.put("sort_direction", "desc");

        basePositiveHasItemsCheck(
                "products.name",
                expectedProductName,
                "",
                PRODUCT_LIST_PAGINATION_SCHEMA,
                params
        );
    }

    void checkHttpStatusCodeInResponse(String url, Map<String, ?> httpQueryParams, int httpStatusCode) {
        given(specification)
                .queryParams(httpQueryParams)
                .get(url)
                .then()
                .statusCode(httpStatusCode)
                .body(emptyOrNullString())
                .time(lessThan(MAX_RESPONSE_TIME));
    }

    void baseNegativeCheck(String posUrl, String schema) {
        checkHttpStatusCodeInResponse(posUrl, Collections.emptyMap(), HttpStatus.NOT_FOUND.value(), schema);
    }

    void basePositiveHasItemsCheck(String path, String item, String posUrl, String schema, Map<String, ?> httpQueryParams) {
        checkHttpStatusCodeInResponse(posUrl, httpQueryParams, HttpStatus.OK.value(), schema)
                .body(path, hasItems(item));
    }

    ValidatableResponse checkHttpStatusCodeInResponse(String url,
                                                      Map<String, ?> httpQueryParams,
                                                      int httpStatusCode,
                                                      String schema) {
        return given(specification)
                .queryParams(httpQueryParams)
                .get(url)
                .then()
                .statusCode(httpStatusCode)
                .body(matchesJsonSchemaInClasspath(schema))
                .time(lessThan(MAX_RESPONSE_TIME));
    }

    private String loadProductJsonResource(Resource resourcePath) {
        try {
            Path path = resourcePath.getFile().toPath();
            return Files.readString(path);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load resource: " + resourcePath, exception);
        }
    }
}
