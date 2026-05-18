package com.zufar.icedlatte.product.endpoint;

import com.zufar.icedlatte.test.config.IntegrationTestBase;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@DisplayName("Product filters integration tests")
class ProductFiltersIntegrationTest extends IntegrationTestBase {

    @LocalServerPort
    protected Integer port;

    private RequestSpecification specification;

    @BeforeEach
    void setUp() {
        specification = given()
                .port(port)
                .basePath(ProductsEndpoint.PRODUCTS_URL)
                .accept(ContentType.JSON);
    }

    @Test
    @DisplayName("Should return available brands and sellers")
    void shouldReturnAvailableBrandsAndSellers() {
        given(specification)
                .get("/brands")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("brands", hasSize(org.hamcrest.Matchers.greaterThan(0)));

        given(specification)
                .get("/sellers")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("sellers", hasSize(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Should filter products by keyword")
    void shouldFilterProductsByKeyword() {
        given(specification)
                .queryParam("keyword", "Nitro")
                .get()
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("products.name", hasItem("Nitro Coffee"))
                .body("products", hasSize(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @DisplayName("Should return bad request when min price is greater than max price")
    void shouldReturnBadRequestWhenMinPriceIsGreaterThanMaxPrice() {
        given(specification)
                .queryParam("min_price", "50")
                .queryParam("max_price", "10")
                .get()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", not(nullValue()));
    }

    @Test
    @DisplayName("Should return bad request when brand names contain duplicates")
    void shouldReturnBadRequestWhenBrandNamesContainDuplicates() {
        given(specification)
                .queryParam("brand_names", "Lavazza", "Lavazza")
                .get()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", not(nullValue()));
    }

    @Test
    @DisplayName("Should return bad request for unsupported minimum average rating")
    void shouldReturnBadRequestForUnsupportedMinimumAverageRating() {
        given(specification)
                .queryParam("minimum_average_rating", 6)
                .get()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("detail", not(nullValue()));
    }
}
