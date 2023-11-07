package com.zufar.onlinestore.product.exception.handler;

import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import com.zufar.onlinestore.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductExceptionHandlerTest {

    private ProductExceptionHandler productExceptionHandler;

    @BeforeEach
    void setUp() {
        productExceptionHandler = new ProductExceptionHandler();
    }

    @Test
    @DisplayName("Should return API response with not found status for product not found exception")
    void shouldReturnApiResponseWithProductNotFoundStatus() {
        UUID productId = UUID.randomUUID();
        ProductNotFoundException exception = new ProductNotFoundException(productId);

        ApiResponse<Void> apiResponse = productExceptionHandler.handleProductNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), apiResponse.httpStatusCode());
        assertEquals("The product with productId = " + productId + " is not found.", apiResponse.messages().get(0));
        assertTrue(apiResponse.description().contains("Operation was failed in method: shouldReturnApiResponseWithProductNotFoundStatus that belongs to the class: com.zufar.onlinestore.product.exception.handler.ProductExceptionHandlerTest. Problematic code line: "));
    }
}
