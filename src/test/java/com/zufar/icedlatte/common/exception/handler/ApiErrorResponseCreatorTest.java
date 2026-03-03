package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiErrorResponseCreator unit tests")
class ApiErrorResponseCreatorTest {

    private final ApiErrorResponseCreator creator = new ApiErrorResponseCreator();

    @Test
    @DisplayName("Builds response from string message with correct status code")
    void buildResponse_stringMessage_setsCorrectFields() {
        ApiErrorResponse response = creator.buildResponse("Something went wrong", HttpStatus.BAD_REQUEST);

        assertThat(response.message()).isEqualTo("Something went wrong");
        assertThat(response.httpStatusCode()).isEqualTo(400);
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Builds response from exception using exception message")
    void buildResponse_exception_usesExceptionMessage() {
        RuntimeException ex = new RuntimeException("Resource not found");
        ApiErrorResponse response = creator.buildResponse(ex, HttpStatus.NOT_FOUND);

        assertThat(response.message()).isEqualTo("Resource not found");
        assertThat(response.httpStatusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Builds 500 response for internal server error")
    void buildResponse_internalServerError_returns500() {
        ApiErrorResponse response = creator.buildResponse("Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(response.httpStatusCode()).isEqualTo(500);
    }
}
