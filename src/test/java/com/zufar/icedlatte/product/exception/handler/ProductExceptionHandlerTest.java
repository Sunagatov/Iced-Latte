package com.zufar.icedlatte.product.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductExceptionHandler unit tests")
class ProductExceptionHandlerTest {

    @Mock private ApiErrorResponseCreator apiErrorResponseCreator;

    @InjectMocks
    private ProductExceptionHandler productExceptionHandler;

    @Nested
    @DisplayName("handleProductNotFoundException")
    class HandleProductNotFoundException {

        @Test
        @DisplayName("maps ProductNotFoundException to a 404 response")
        void mapsProductNotFoundExceptionTo404Response() {
            ProductNotFoundException exception = new ProductNotFoundException(UUID.randomUUID());
            ApiErrorResponse response = new ApiErrorResponse(
                    exception.getMessage(),
                    HttpStatus.NOT_FOUND.value(),
                    LocalDateTime.now()
            );
            when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND)).thenReturn(response);

            ApiErrorResponse result = productExceptionHandler.handleProductNotFoundException(exception);

            assertThat(result).isSameAs(response);
            verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.NOT_FOUND);
            verifyNoMoreInteractions(apiErrorResponseCreator);
        }
    }

    @Nested
    @DisplayName("handleGetProductsBadRequestException")
    class HandleGetProductsBadRequestException {

        @Test
        @DisplayName("maps GetProductsBadRequestException to a 400 response")
        void mapsGetProductsBadRequestExceptionTo400Response() {
            GetProductsBadRequestException exception = new GetProductsBadRequestException("sort property is invalid");
            ApiErrorResponse response = new ApiErrorResponse(
                    exception.getMessage(),
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now()
            );
            when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST)).thenReturn(response);

            ApiErrorResponse result = productExceptionHandler.handleGetProductsBadRequestException(exception);

            assertThat(result).isSameAs(response);
            verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.BAD_REQUEST);
            verifyNoMoreInteractions(apiErrorResponseCreator);
        }
    }
}
