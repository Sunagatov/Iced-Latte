package com.zufar.onlinestore.product.endpoint.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.onlinestore.product.endpoint.ProductsEndpoint;
import io.restassured.RestAssured;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;


@Testcontainers
class ProductsEndpointTest extends BaseProductEndpointTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
            .parse("mockserver/mockserver")
            .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

    @Rule
    public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

    @Autowired
    private ObjectMapper objectMapper;

    @Value("classpath:product/model/product-base-model.json")
    private Resource productResponseResource;

    @Value("classpath:product/model/product-error-model.json")
    private Resource errorResponseResource;

    @Value("classpath:product/model/product-pagination-americano-model.json")
    private Resource paginationAmericanoResponseResource;

    @Value("classpath:product/model/product-pagination-macchiato-model.json")
    private Resource paginationMacchiatoResponseResource;

    static MockServerClient mockServerClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        mockServer.start();
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mockServerClient.reset();
    }

    @Test
    void shouldRetrieveProductSuccessfullyById() throws IOException {
        String productId = "d2160f5d-b8e1-4cbe-8bb2-2f2f48484848";
        String mockResponse = loadProductJsonResource(productResponseResource);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL + "/" + productId))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withBody(mockResponse));

        checkStatusCodeInResponse("/" + productId, HttpStatus.OK.value(), "product/model/schema/product-schema.json");
    }

    @Test
    void shouldReturnNotFoundForInvalidProductId() throws IOException {
        String invalidProductId = "f1a1d5c1-a6f7-4e2b-8cfd-3d3f52e1abcd";
        String mockErrorResponse = loadProductJsonResource(errorResponseResource);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withPath(ProductsEndpoint.PRODUCTS_URL + "/" + invalidProductId))
                .respond(response()
                        .withStatusCode(HttpStatus.NOT_FOUND.value())
                        .withBody(mockErrorResponse));

        baseNegativeCheck("/" + invalidProductId, "product/model/schema/product-failed-schema.json");
    }

    @Test
    void shouldFetchProductsWithPaginationAndSorting() throws IOException {
        String mockResponse = loadProductJsonResource(paginationMacchiatoResponseResource);

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

        checkStatusCodeInResponse("?page=1&size=1&sort_attribute=name&sort_direction=desc", HttpStatus.OK.value(), "product/model/schema/product-list-pagination-schema.json");
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
    void shouldContainProductWithNameAmericano() throws IOException {
        String expectedProductName = "Americano";
        String mockResponse = loadProductJsonResource(paginationAmericanoResponseResource);

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

        basePositiveHasItemsCheck("products.name", expectedProductName, "?page=5&size=1&sort_attribute=name&sort_direction=desc", "product/model/schema/product-list-pagination-schema.json");
    }

    private String loadProductJsonResource(Resource resource) throws IOException {
        return Files.readString(resource.getFile().toPath());
    }
}
