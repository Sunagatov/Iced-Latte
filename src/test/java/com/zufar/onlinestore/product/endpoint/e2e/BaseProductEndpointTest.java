package com.zufar.onlinestore.product.endpoint.e2e;

import com.zufar.onlinestore.product.endpoint.ProductsEndpoint;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThan;

public class BaseProductEndpointTest {

    static RequestSpecification specification;

    @BeforeAll
    static void baseSetUp() {
        specification = given()
                .baseUri(ProductsEndpoint.PRODUCTS_URL)
                .accept(ContentType.JSON);
    }

    public static ValidatableResponse checkStatusCodeInResponse(String url, int code, String schema)
    {
        return given(specification)
                .get(url)
                .then()
                .statusCode(code)
                .body(matchesJsonSchemaInClasspath(schema))
                .time(lessThan(1500L));
    }

    public static void checkStatusCodeInResponse(String url, int code){
        given(specification)
                .get(url)
                .then()
                .statusCode(code)
                .body(emptyOrNullString())
                .time(lessThan(1500L));
    }

    public void baseNegativeCheck(String posUrl, String schema) {
        checkStatusCodeInResponse(posUrl, HttpStatus.NOT_FOUND.value(), schema);
    }

    public void basePositiveHasItemsCheck(String path, String item, String posUrl, String schema) {
        checkStatusCodeInResponse(posUrl, HttpStatus.OK.value(), schema)
                .body(path, hasItems(item));
    }
}
