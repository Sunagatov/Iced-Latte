package com.zufar.onlinestore.product.endpoint.e2e;

import com.zufar.onlinestore.test.config.AbstractE2ETest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zufar.onlinestore.test.config.RestAssertion.*;
import static io.restassured.RestAssured.given;


@Testcontainers
class ProductsEndpointTest extends AbstractE2ETest {

    @Test
    void shouldRetrieveProductSuccessfullyById() {
        String productId = "418499f3-d951-40bf-9414-5cb90ab21ecb";
        Response response = given(specification)
                .get("/" + productId);
        checkStatusCodeInResponse(response, HttpStatus.OK.value(), "product/model/schema/product-schema.json");
    }

    @Test
    void shouldReturnNotFoundForInvalidProductId() {
        String invalidProductId = UUID.randomUUID().toString();
        Response response = given(specification)
                .get("/" + invalidProductId);
        baseNotFoundCheck(response, "product/model/schema/product-failed-schema.json");
    }

    @Test
    void shouldFetchProductsWithPaginationAndSorting() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 1);
        params.put("sort_attribute", "name");
        params.put("sort_direction", "desc");
        Response response = given(specification)
                .queryParams(params)
                .get();
        checkStatusCodeInResponse(response, HttpStatus.OK.value(), "product/model/schema/product-list-pagination-schema.json");
    }

    @Test
    void shouldReturnErrorForInvalidSortAttribute() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 15);
        params.put("size", 8);
        params.put("sort_attribute", "names");
        params.put("sort_direction", "desc");
        Response response = given(specification)
                .queryParams(params)
                .get();
        checkStatusCodeInResponse(response, HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldContainProductWithNameAmericano() {
        String expectedProductName = "Americano";
        Map<String, Object> params = new HashMap<>();
        params.put("page", 5);
        params.put("size", 1);
        params.put("sort_attribute", "name");
        params.put("sort_direction", "desc");
        Response response = given(specification)
                .queryParams(params)
                .get();
        baseOKHasItemsCheck(
                response,
                "products.name",
                expectedProductName,
                "product/model/schema/product-list-pagination-schema.json"
        );
    }
}
