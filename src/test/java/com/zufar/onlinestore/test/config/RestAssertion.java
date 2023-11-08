package com.zufar.onlinestore.test.config;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.springframework.http.HttpStatus;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public final class RestAssertion {

    public static ValidatableResponse assertRestApiBodySchemaResponse(Response response, int code, String schema) {
        return response.then()
                .statusCode(code)
                .body(matchesJsonSchemaInClasspath(schema))
                .time(lessThan(1500L));
    }

    public static void assertRestApiEmptyBodyResponse(Response response, int code) {
        response.then()
                .statusCode(code)
                .body(emptyOrNullString())
                .time(lessThan(1500L));
    }

    public static void assertRestApiNotFoundResponse(Response response, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.NOT_FOUND.value(), schema);
    }

    public static void assertRestApiOkResponse(Response response, String path, String item, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.OK.value(), schema)
                .body(path, hasItems(item));
    }
}
