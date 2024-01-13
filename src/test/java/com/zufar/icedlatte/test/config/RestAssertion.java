package com.zufar.icedlatte.test.config;

import io.restassured.filter.log.LogDetail;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matcher;
import org.springframework.http.HttpStatus;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThan;

public final class RestAssertion {

    public static final long DEFAULT_HTTP_TIMEOUT = 2000L;

    public static void assertRestApiBadRequestResponse(Response response, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.BAD_REQUEST, schema);
    }

    public static void assertRestApiNotFoundResponse(Response response, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.NOT_FOUND, schema);
    }

    public static void assertRestApiCreateResponse(Response response, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.CREATED, schema);
    }

    public static void assertRestApiUnAuthorizedResponse(Response response, String schema) {
        assertRestApiBodySchemaResponse(response, HttpStatus.UNAUTHORIZED, schema);
    }

    public static void assertRestApiEmptyBodyResponse(Response response, HttpStatus httpStatusCode) {
        assertRestApiBodySchemaMatcherResponse(response, httpStatusCode, emptyOrNullString());
    }

    public static ValidatableResponse assertRestApiBodySchemaResponse(Response response, HttpStatus httpStatusCode, String schema) {
        return assertRestApiBodySchemaMatcherResponse(response, httpStatusCode, matchesJsonSchemaInClasspath(schema));
    }

    private static ValidatableResponse assertRestApiBodySchemaMatcherResponse(Response response, HttpStatus httpStatusCode, Matcher<?> schemaMatcher) {
        return response.then()
                .log()
                .ifValidationFails(LogDetail.BODY)
                .statusCode(httpStatusCode.value())
                .body(schemaMatcher)
                .time(lessThan(DEFAULT_HTTP_TIMEOUT));
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
