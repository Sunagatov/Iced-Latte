package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiErrorResponseCreator unit tests")
class ApiErrorResponseCreatorTest {

    private final ApiErrorResponseCreator creator = new ApiErrorResponseCreator();

    @Nested
    @DisplayName("buildResponse")
    class BuildResponse {

        @Test
        @DisplayName("builds response from string message with current timestamp and status code")
        void buildsResponseFromStringMessageWithCurrentTimestampAndStatusCode() {
            ApiErrorResponse response = creator.buildResponse("Something went wrong", HttpStatus.BAD_REQUEST);

            assertThat(response.message()).isEqualTo("Something went wrong");
            assertThat(response.httpStatusCode()).isEqualTo(400);
            assertThat(response.timestamp()).isNotNull();
            assertThat(response.errors()).isEmpty();
        }

        @Test
        @DisplayName("builds response from exception message")
        void buildsResponseFromExceptionMessage() {
            RuntimeException ex = new RuntimeException("Resource not found");

            ApiErrorResponse response = creator.buildResponse(ex, HttpStatus.NOT_FOUND);

            assertThat(response.message()).isEqualTo("Resource not found");
            assertThat(response.httpStatusCode()).isEqualTo(404);
            assertThat(response.timestamp()).isNotNull();
        }
    }
}
