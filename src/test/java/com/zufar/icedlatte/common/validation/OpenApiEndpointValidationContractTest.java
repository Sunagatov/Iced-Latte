package com.zufar.icedlatte.common.validation;

import com.zufar.icedlatte.cart.endpoint.CartEndpoint;
import com.zufar.icedlatte.favorite.endpoint.FavoritesEndpoint;
import com.zufar.icedlatte.order.endpoint.AdminOrderEndpoint;
import com.zufar.icedlatte.order.endpoint.OrderEndpoint;
import com.zufar.icedlatte.payment.endpoint.PaymentEndpoint;
import com.zufar.icedlatte.product.endpoint.ProductsEndpoint;
import com.zufar.icedlatte.review.endpoint.ProductReviewEndpoint;
import com.zufar.icedlatte.security.endpoint.UserSecurityEndpoint;
import com.zufar.icedlatte.user.endpoint.UserEndpoint;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

@DisplayName("OpenAPI endpoint validation contract tests")
class OpenApiEndpointValidationContractTest {

    private static final List<Class<?>> GENERATED_API_ENDPOINTS = List.of(
            CartEndpoint.class,
            FavoritesEndpoint.class,
            AdminOrderEndpoint.class,
            OrderEndpoint.class,
            PaymentEndpoint.class,
            ProductsEndpoint.class,
            ProductReviewEndpoint.class,
            UserEndpoint.class,
            UserSecurityEndpoint.class
    );

    @Test
    @DisplayName("endpoint validation metadata matches implemented generated OpenAPI interfaces")
    void endpointValidationMetadataMatchesGeneratedApiInterfaces() {
        assertThatCode(() -> {
            try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
                var executableValidator = validatorFactory.getValidator().forExecutables();
                for (Class<?> endpointClass : GENERATED_API_ENDPOINTS) {
                    Object endpoint = instantiateWithMocks(endpointClass);
                    for (Method method : endpointClass.getMethods()) {
                        if (isGeneratedOpenApiMethod(endpointClass, method)) {
                            executableValidator.validateParameters(endpoint, method, dummyArguments(method));
                        }
                    }
                }
            }
        }).doesNotThrowAnyException();
    }

    private static boolean isGeneratedOpenApiMethod(Class<?> endpointClass, Method method) {
        for (Class<?> apiInterface : endpointClass.getInterfaces()) {
            if (!apiInterface.getPackageName().startsWith("com.zufar.icedlatte.openapi.")) {
                continue;
            }
            try {
                apiInterface.getMethod(method.getName(), method.getParameterTypes());
                return true;
            } catch (NoSuchMethodException ignored) {
                // Not an OpenAPI operation method.
            }
        }
        return false;
    }

    private static Object instantiateWithMocks(Class<?> endpointClass) throws ReflectiveOperationException {
        Constructor<?> constructor = endpointClass.getDeclaredConstructors()[0];
        Object[] args = new Object[constructor.getParameterCount()];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = mock(parameterTypes[i]);
        }
        return constructor.newInstance(args);
    }

    private static Object[] dummyArguments(Method method) throws ReflectiveOperationException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = dummyValue(parameterTypes[i]);
        }
        return args;
    }

    private static Object dummyValue(Class<?> parameterType) throws ReflectiveOperationException {
        if (parameterType == String.class) {
            return "dummy";
        }
        if (parameterType == Integer.class || parameterType == int.class) {
            return 1;
        }
        if (parameterType == UUID.class) {
            return UUID.randomUUID();
        }
        if (parameterType == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (parameterType == LocalDate.class) {
            return LocalDate.now();
        }
        if (parameterType == List.class) {
            return List.of();
        }
        if (parameterType == MultipartFile.class) {
            return mock(MultipartFile.class);
        }
        return parameterType.getDeclaredConstructor().newInstance();
    }
}
