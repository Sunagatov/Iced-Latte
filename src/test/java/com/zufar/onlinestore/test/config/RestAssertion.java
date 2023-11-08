package com.zufar.onlinestore.test.config;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matcher;
import org.springframework.http.HttpStatus;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public final class RestAssertion {

    public static final long DEFAULT_HTTP_TIMEOUT = 2000L;

    private static ValidatableResponse assertRestApiBodySchemaMatcherResponse(Response response, HttpStatus httpStatusCode, Matcher<?> schemaMatcher) {
        return response.then()
                .statusCode(httpStatusCode.value())
                .body(schemaMatcher)
                .time(lessThan(DEFAULT_HTTP_TIMEOUT));
    }

    private static ValidatableResponse assertRestApiBodySchemaResponse(Response response, HttpStatus httpStatusCode, String schema) {
        return assertRestApiBodySchemaMatcherResponse(response, httpStatusCode, matchesJsonSchemaInClasspath(schema));
    }

    private static void assertRestApiEmptyBodyResponse(Response response, HttpStatus httpStatusCode) {
        assertRestApiBodySchemaMatcherResponse(response, httpStatusCode, emptyOrNullString());
    }

    public static void assertRestApiForbiddenEmptyResponse(Response response) {
        assertRestApiEmptyBodyResponse(response, HttpStatus.FORBIDDEN);
    }

    public static void assertRestApiNotFoundResponse(Response response, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.NOT_FOUND, schema);
    }

    public static void assertRestApiOkResponse(Response response, String schema) {
        assertRestApiOkResponse(response, schema, null, null);
    }

    public static void assertRestApiOkResponse(Response response, String schema, String path, String item) {
        ValidatableResponse validatableResponse = assertRestApiBodySchemaResponse(response, HttpStatus.OK, schema);
        if (path != null) {
            validatableResponse.body(path, hasItems(item));
        }
    }
}
