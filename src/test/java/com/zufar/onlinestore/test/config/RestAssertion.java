package com.zufar.onlinestore.test.config;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.springframework.http.HttpStatus;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public final class RestAssertion {

    public static ValidatableResponse checkStatusCodeInResponse(Response response, int code, String schema) {
        return response.then()
                .statusCode(code)
                .body(matchesJsonSchemaInClasspath(schema))
                .time(lessThan(1500L));
    }

    public static void checkStatusCodeInResponse(Response response, int code) {
        response.then()
                .statusCode(code)
                .body(emptyOrNullString())
                .time(lessThan(1500L));
    }

    public static void baseNotFoundCheck(Response response, String schema) {
        checkStatusCodeInResponse(response, HttpStatus.NOT_FOUND.value(), schema);
    }

    public static void baseOKHasItemsCheck(Response response, String path, String item, String schema) {
        checkStatusCodeInResponse(response, HttpStatus.OK.value(), schema)
                .body(path, hasItems(item));
    }
}
