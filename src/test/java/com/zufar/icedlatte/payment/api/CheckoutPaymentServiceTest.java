package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutPaymentService unit tests")
class CheckoutPaymentServiceTest {

    @Mock @SuppressWarnings("unused") private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock @SuppressWarnings("unused") private CheckoutPaymentTransactionService txService;
    @Mock @SuppressWarnings("unused") private StripeCheckoutSessionCreator stripeSessionCreator;
    @Mock @SuppressWarnings("unused") private StripeProperties stripeProperties;
    @InjectMocks private CheckoutPaymentService service;

    private final CreateCheckoutRequestDto request = new CreateCheckoutRequestDto()
            .recipientName("A").recipientSurname("B");

    @Test
    @DisplayName("rejects null Idempotency-Key")
    void checkout_nullKey_throws() {
        assertThatThrownBy(() -> service.checkout(request, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("rejects blank Idempotency-Key")
    void checkout_blankKey_throws() {
        assertThatThrownBy(() -> service.checkout(request, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("rejects Idempotency-Key longer than 100 characters")
    void checkout_longKey_throws() {
        String longKey = "a".repeat(101);
        assertThatThrownBy(() -> service.checkout(request, longKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100 characters");
    }
}
