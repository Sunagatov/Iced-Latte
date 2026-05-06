package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentProvider;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutPaymentTransactionService unit tests")
class CheckoutPaymentTransactionServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderCreator orderCreator;
    @Mock private ShoppingCartService shoppingCartService;
    @Mock private StripeProperties stripeProperties;
    @InjectMocks private CheckoutPaymentTransactionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String IDEMPOTENCY_KEY = "test-key-123";

    @Test
    @DisplayName("prepareCheckout creates order and payment for new checkout")
    void prepareCheckout_newCheckout_createsOrderAndPayment() {
        CreateCheckoutRequestDto request = new CreateCheckoutRequestDto()
                .recipientName("John").recipientSurname("Doe");

        var productInfo = new com.zufar.icedlatte.openapi.dto.ProductInfoDto()
                .id(UUID.randomUUID()).name("Coffee").price(BigDecimal.valueOf(12.50));
        var cartItem = new com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto()
                .productInfo(productInfo).productQuantity(2);
        ShoppingCartDto cart = new ShoppingCartDto()
                .items(List.of(cartItem)).itemsTotalPrice(BigDecimal.valueOf(25.00)).itemsQuantity(2);

        Order order = Order.builder().id(UUID.randomUUID()).userId(USER_ID)
                .itemsTotalPrice(BigDecimal.valueOf(25.00)).build();

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.empty());
        when(shoppingCartService.getByUserIdOrThrow(USER_ID)).thenReturn(cart);
        when(orderCreator.createPendingPaymentOrder(eq(USER_ID), eq(request), eq(cart)))
                .thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeProperties.getCurrency()).thenReturn("usd");

        CheckoutPreparation result = service.prepareCheckout(USER_ID, request, IDEMPOTENCY_KEY);

        assertThat(result.existing()).isFalse();
        assertThat(result.order()).isEqualTo(order);
        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(result.payment().getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(result.payment().getCurrency()).isEqualTo("usd");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCheckoutIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("prepareCheckout returns existing order/payment on idempotent retry")
    void prepareCheckout_idempotentHit_returnsExisting() {
        UUID orderId = UUID.randomUUID();
        Payment existingPayment = Payment.builder().orderId(orderId).userId(USER_ID)
                .providerSessionId("cs_test_existing").status(PaymentStatus.STRIPE_SESSION_CREATED).build();
        Order existingOrder = Order.builder().id(orderId).userId(USER_ID).build();

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.of(existingPayment));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        CheckoutPreparation result = service.prepareCheckout(
                USER_ID, new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B"),
                IDEMPOTENCY_KEY);

        assertThat(result.existing()).isTrue();
        assertThat(result.order()).isEqualTo(existingOrder);
        assertThat(result.payment()).isEqualTo(existingPayment);
        assertThat(result.cartItems()).isEmpty();
        verify(shoppingCartService, never()).getByUserIdOrThrow(any());
    }

    @Test
    @DisplayName("prepareCheckout uses fetch join for items when providerSessionId is null (retry before Stripe call)")
    void prepareCheckout_idempotentHit_noSessionId_usesFetchJoin() {
        UUID orderId = UUID.randomUUID();
        Payment existingPayment = Payment.builder().orderId(orderId).userId(USER_ID)
                .providerSessionId(null).status(PaymentStatus.CREATED).build();
        Order existingOrder = Order.builder().id(orderId).userId(USER_ID).build();

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.of(existingPayment));
        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(existingOrder));

        CheckoutPreparation result = service.prepareCheckout(
                USER_ID, new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B"),
                IDEMPOTENCY_KEY);

        assertThat(result.existing()).isTrue();
        verify(orderRepository).findByIdWithItems(orderId);
        verify(orderRepository, never()).findById(orderId);
    }

    @Test
    @DisplayName("prepareCheckout throws BadRequestException for empty cart")
    void prepareCheckout_emptyCart_throws() {
        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.empty());
        when(shoppingCartService.getByUserIdOrThrow(USER_ID))
                .thenReturn(new ShoppingCartDto().items(List.of()));

        assertThatThrownBy(() -> service.prepareCheckout(
                USER_ID, new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B"),
                IDEMPOTENCY_KEY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("saveStripeDetails updates payment with session ID and status")
    void saveStripeDetails_updatesPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder().id(paymentId).status(PaymentStatus.CREATED).build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveStripeDetails(paymentId, new StripeSessionResult("cs_test_123", "https://checkout.stripe.com/..."));

        assertThat(payment.getProviderSessionId()).isEqualTo("cs_test_123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.STRIPE_SESSION_CREATED);
        verify(paymentRepository).save(payment);
    }
}
