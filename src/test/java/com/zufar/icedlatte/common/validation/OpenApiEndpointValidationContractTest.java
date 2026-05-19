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
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("every generated OpenAPI operation is explicitly overridden by its endpoint")
    void everyGeneratedOpenApiOperationHasConcreteEndpointOverride() {
        for (Class<?> endpointClass : GENERATED_API_ENDPOINTS) {
            generatedOpenApiOperationMethods(endpointClass).forEach(apiMethod -> assertThatCode(() -> {
                        Method endpointMethod = endpointClass.getDeclaredMethod(apiMethod.getName(), apiMethod.getParameterTypes());
                        assertThat(endpointMethod).isNotNull();
                    })
                    .as("%s must override generated OpenAPI operation %s",
                            endpointClass.getSimpleName(), apiMethod.getName())
                    .doesNotThrowAnyException());
        }
    }

    @Test
    @DisplayName("endpoint validation metadata matches implemented generated OpenAPI interfaces")
    void endpointValidationMetadataMatchesGeneratedApiInterfaces() {
        assertThatCode(() -> {
            try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
                var executableValidator = validatorFactory.getValidator().forExecutables();
                for (Class<?> endpointClass : GENERATED_API_ENDPOINTS) {
                    Object endpoint = instantiateWithMocks(endpointClass);
                    for (Method apiMethod : generatedOpenApiOperationMethods(endpointClass).toList()) {
                        Method endpointMethod = endpointClass.getMethod(apiMethod.getName(), apiMethod.getParameterTypes());
                        executableValidator.validateParameters(endpoint, endpointMethod, dummyArguments(endpointMethod));
                    }
                }
            }
        }).doesNotThrowAnyException();
    }

    private static Stream<Method> generatedOpenApiOperationMethods(Class<?> endpointClass) {
        return Arrays.stream(endpointClass.getInterfaces())
                .filter(OpenApiEndpointValidationContractTest::isGeneratedOpenApiInterface)
                .flatMap(apiInterface -> Arrays.stream(apiInterface.getMethods()))
                .filter(Method::isDefault)
                .filter(method -> ResponseEntity.class.isAssignableFrom(method.getReturnType()))
                .peek(method -> assertThat(method.getDeclaringClass().getPackageName())
                        .startsWith("com.zufar.icedlatte.openapi."));
    }

    private static boolean isGeneratedOpenApiInterface(Class<?> apiInterface) {
        return apiInterface.isInterface()
                && apiInterface.getPackageName().startsWith("com.zufar.icedlatte.openapi.");
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
        if (parameterType == URI.class) {
            return URI.create("https://example.com/callback");
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
