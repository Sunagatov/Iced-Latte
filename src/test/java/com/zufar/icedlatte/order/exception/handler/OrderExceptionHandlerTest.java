package com.zufar.icedlatte.order.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExceptionHandler unit tests")
class OrderExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private OrderExceptionHandler handler;

    @Test
    @DisplayName("Returns BAD_REQUEST with supported statuses in message")
    void handleTypeMismatch_returnsBadRequestWithStatuses() throws Exception {
        Method method = Object.class.getDeclaredMethod("toString");
        MethodParameter mp = new MethodParameter(method, -1);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "INVALID", OrderStatus.class, "status", mp, new RuntimeException());
        String expectedDetail = "Incorrect status value. Supported: " + Arrays.toString(OrderStatus.values());
        ProblemDetail expected = ProblemDetail.forStatus(400);
        when(problemDetailFactory.build("invalid-parameter", "Invalid parameter",
                HttpStatus.BAD_REQUEST, expectedDetail)).thenReturn(expected);

        ProblemDetail result = handler.handleTypeMismatch(ex);

        assertThat(result).isEqualTo(expected);
    }
}
