package com.zufar.icedlatte.product.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductExceptionHandler Tests")
class ProductExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private ProductExceptionHandler handler;

    @Test
    @DisplayName("Should return NOT_FOUND for ProductNotFoundException")
    void shouldReturnNotFoundWhenProductIsMissing() {
        ProductNotFoundException exception = new ProductNotFoundException(UUID.randomUUID());
        ProblemDetail expected = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        when(problemDetailFactory.build(
                        ProblemType.PRODUCT_NOT_FOUND,
                        "Product not found",
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()))
                .thenReturn(expected);

        ResponseEntity<ProblemDetail> result = handler.handleProductNotFoundException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo(expected);
    }
}
