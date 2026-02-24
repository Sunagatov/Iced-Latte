package com.zufar.icedlatte.order.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExceptionHandler unit tests")
class OrderExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;
    @InjectMocks
    private OrderExceptionHandler handler;

    @Test
    @DisplayName("Returns BAD_REQUEST with supported statuses in message")
    void handleTypeMismatch_returnsBadRequestWithStatuses() throws Exception {
        Method method = Object.class.getDeclaredMethod("toString");
        MethodParameter mp = new MethodParameter(method, -1);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "INVALID", OrderStatus.class, "status", mp, new RuntimeException());
        String expectedMsg = "Incorrect status value. Supported status: " + Arrays.toString(OrderStatus.values());
        ApiErrorResponse expected = new ApiErrorResponse(expectedMsg, 400, LocalDateTime.now());

        when(apiErrorResponseCreator.buildResponse(contains("Incorrect status value"), eq(HttpStatus.BAD_REQUEST)))
                .thenReturn(expected);

        ApiErrorResponse result = handler.handleMethodArgumentNotValidException(ex);

        assertThat(result).isEqualTo(expected);
    }
}
