package com.zufar.onlinestore.product.endpoint.e2e;

import com.zufar.onlinestore.product.endpoint.ProductsEndpoint;
import io.restassured.RestAssured;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductsEndpointTest extends BaseProductEndpointTest {

    @LocalServerPort
    private Integer port;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
            .parse("mockserver/mockserver")
            .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

    @Rule
    public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

    static MockServerClient mockServerClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mockServerClient.reset();
    }

    @Test
    void shouldRetrieveProductSuccessfullyById() {
        String productId = "d2160f5d-b8e1-4cbe-8bb2-2f2f48484848";
        String mockResponse = """
                {
                    "id": "%s",
                    "name": "Test Product",
                    "description": "Test Product Description",
                    "price": 10.5,
                    "quantity": 5,
                    "active": true
                }
                """.formatted(productId);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL + "/" + productId))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse));

        checkStatusCodeInResponse("/" + productId, HttpStatus.OK.value(), "model/product-schema.json");
    }

    @Test
    void shouldReturnNotFoundForInvalidProductId() {
        String invalidProductId = "f1a1d5c1-a6f7-4e2b-8cfd-3d3f52e1abcd";
        String mockErrorResponse = """
                {
                    "data": null,
                    "message": ["Product not found"],
                    "description": "The product with the given ID does not exist",
                    "httpStatusCode": 404,
                    "timestamp": "2023-10-15T18:30:00Z"
                }
                """;

        mockServerClient.when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL + "/" + invalidProductId))
                .respond(response()
                        .withStatusCode(HttpStatus.NOT_FOUND.value())
                        .withBody(mockErrorResponse));

        baseNegativeCheck("/" + invalidProductId, "model/negative-response-schema.json");
    }

    @Test
    void shouldFetchProductsWithPaginationAndSorting() {
        String mockResponse = """
                {
                    "products": [
                        {
                            "id": "46f97165-00a7-4b45-9e5c-09f8168b0047",
                            "name": "Macchiato",
                            "description": "Espresso with a Dash of Frothy Milk",
                            "price": 3.99,
                            "quantity": 90,
                            "active": true
                        }
                    ],
                    "page": 1,
                    "size": 1,
                    "totalElements": 6,
                    "totalPages": 6
                }""";

        mockServerClient.when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL)
                        .withQueryStringParameters(
                                param("page", "1"),
                                param("size", "1"),
                                param("sort_attribute", "name"),
                                param("sort_direction", "desc")
                        ))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse));

        checkStatusCodeInResponse("?page=1&size=1&sort_attribute=name&sort_direction=desc", HttpStatus.OK.value(), "/model/product-list-pagination-schema.json");
    }

    @Test
    void shouldReturnErrorForInvalidSortAttribute() {

        mockServerClient.when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL)
                        .withQueryStringParameters(
                                param("page", "15"),
                                param("size", "8"),
                                param("sort_attribute", "names"),
                                param("sort_direction", "desc")
                        ))
                .respond(response()
                        .withStatusCode(HttpStatus.FORBIDDEN.value()));

        checkStatusCodeInResponse("?page=15&size=8&sort_attribute=names&sort_direction=desc", HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldContainProductWithNameAmericano() {
        String expectedProductName = "Americano";
        String mockResponse = """
                            {
                                "products": [
                                    {
                                        "id": "e6a4d7f2-d40e-4e5f-93b8-5d56ce6724c5",
                                        "name": "Americano",
                                        "description": "Espresso Diluted with Hot Water",
                                        "price": 4.49,
                                        "quantity": 70,
                                        "active": true
                                    }
                                ],
                                "page": 5,
                                "size": 1,
                                "totalElements": 6,
                                "totalPages": 6
                            }
                """;

        mockServerClient.when(
                        request()
                                .withMethod(HttpMethod.GET.name())
                                .withPath(ProductsEndpoint.PRODUCTS_URL)
                                .withQueryStringParameter("page", "5")
                                .withQueryStringParameter("size", "1")
                                .withQueryStringParameter("sort_attribute", "name")
                                .withQueryStringParameter("sort_direction", "desc")
                )
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse)
                );

        basePositiveHasItemsCheck("products.name", expectedProductName, "?page=5&size=1&sort_attribute=name&sort_direction=desc", "model/products-pagination-schema.json");
    }
}
