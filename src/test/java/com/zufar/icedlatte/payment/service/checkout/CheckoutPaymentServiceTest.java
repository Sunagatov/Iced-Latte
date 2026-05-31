package com.zufar.icedlatte.payment.service.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CheckoutResponseDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.payment.converter.StripeSessionLineItemListConverter;
import com.zufar.icedlatte.payment.dto.CheckoutPaymentSnapshot;
import com.zufar.icedlatte.payment.dto.CheckoutPreparation;
import com.zufar.icedlatte.payment.dto.StripeSessionResult;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutPaymentService unit tests")
class CheckoutPaymentServiceTest {

    @Mock
    @SuppressWarnings("unused")
    private CurrentUserProvider currentUserProvider;

    @Mock
    @SuppressWarnings("unused")
    private CheckoutPaymentTransactionService txService;

    @Mock
    @SuppressWarnings("unused")
    private StripeCheckoutSessionCreator stripeSessionCreator;

    @Mock
    @SuppressWarnings("unused")
    private StripeSessionGateway stripeSessionGateway;

    @Mock
    @SuppressWarnings("unused")
    private StripeSessionLineItemListConverter lineItemConverter;

    @InjectMocks
    private CheckoutPaymentService service;

    private final CreateCheckoutRequestDto request =
            new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B");

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

    @Test
    @DisplayName("retries existing checkout after idempotency unique-key collision")
    void checkout_idempotencyCollision_returnsExistingCheckout() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        CheckoutPaymentSnapshot payment = new CheckoutPaymentSnapshot(paymentId, null);
        OrderSnapshot order = new OrderSnapshot(
                orderId, userId, OrderStatusSnapshot.PENDING_PAYMENT, BigDecimal.TEN, null, List.of());
        CheckoutPreparation existing = new CheckoutPreparation.ExistingCheckout(order, payment);

        when(currentUserProvider.get()).thenReturn(new CurrentUserSnapshot(userId, "user@example.com"));
        when(txService.prepareCheckout(userId, request, "same-key"))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(txService.findExistingCheckout(userId, "same-key")).thenReturn(Optional.of(existing));
        when(stripeSessionCreator.createFromLineItems(eq(order), eq("user@example.com"), eq(List.of())))
                .thenReturn(new StripeSessionResult("cs_test_1", "https://checkout.stripe.test/session"));

        CheckoutResponseDto response = service.checkout(request, "same-key");

        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getCheckoutUrl()).isEqualTo(URI.create("https://checkout.stripe.test/session"));
        verify(txService)
                .saveStripeDetails(
                        paymentId, new StripeSessionResult("cs_test_1", "https://checkout.stripe.test/session"));
    }
}
