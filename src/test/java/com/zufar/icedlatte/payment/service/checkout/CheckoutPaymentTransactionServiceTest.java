package com.zufar.icedlatte.payment.service.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.order.api.OrderCheckoutApi;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.payment.dto.CheckoutPaymentSnapshot;
import com.zufar.icedlatte.payment.dto.CheckoutPreparation;
import com.zufar.icedlatte.payment.dto.StripeSessionResult;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentProvider;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutPaymentTransactionService unit tests")
class CheckoutPaymentTransactionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderPaymentApi orderPaymentApi;

    @Mock
    private OrderCheckoutApi orderCheckoutApi;

    @Mock
    private CartCheckoutApi shoppingCartService;

    @Mock
    private StripeProperties stripeProperties;

    @InjectMocks
    private CheckoutPaymentTransactionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String IDEMPOTENCY_KEY = "test-key-123";

    @Test
    @DisplayName("prepareCheckout creates order and payment for new checkout")
    void prepareCheckout_newCheckout_createsOrderAndPayment() {
        CreateCheckoutRequestDto request =
                new CreateCheckoutRequestDto().recipientName("John").recipientSurname("Doe");

        var productInfo = new ProductSnapshot(
                UUID.randomUUID(),
                "Coffee",
                "Desc",
                BigDecimal.valueOf(12.50),
                10,
                true,
                null,
                BigDecimal.valueOf(4.5),
                12,
                "Brand",
                "Seller",
                250);
        var cartItem = new CartItemSnapshot(UUID.randomUUID(), productInfo, 2);
        CartSnapshot cart = new CartSnapshot(
                UUID.randomUUID(),
                USER_ID,
                List.of(cartItem),
                1,
                BigDecimal.valueOf(25.00),
                2,
                OffsetDateTime.now(),
                null);

        OrderSnapshot order = new OrderSnapshot(
                UUID.randomUUID(),
                USER_ID,
                OrderStatusSnapshot.PENDING_PAYMENT,
                java.math.BigDecimal.valueOf(25.00),
                null,
                java.util.List.of());

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.empty());
        when(shoppingCartService.getByUserIdOrThrow(USER_ID)).thenReturn(cart);
        when(orderCheckoutApi.createPendingPaymentOrderSnapshot(eq(USER_ID), any(CheckoutOrderRequest.class), eq(cart)))
                .thenReturn(order);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });
        when(stripeProperties.currency()).thenReturn("usd");

        CheckoutPreparation result = service.prepareCheckout(USER_ID, request, IDEMPOTENCY_KEY);

        assertThat(result).isInstanceOf(CheckoutPreparation.NewCheckout.class);
        CheckoutPreparation.NewCheckout newCheckout = (CheckoutPreparation.NewCheckout) result;
        assertThat(result.order()).isEqualTo(order);
        assertThat(result.payment().id()).isNotNull();
        assertThat(result.payment().providerSessionId()).isNull();
        assertThat(newCheckout.cartItems()).containsExactly(cartItem);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCheckoutIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(captor.getValue().getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(captor.getValue().getCurrency()).isEqualTo("usd");

        ArgumentCaptor<CheckoutOrderRequest> orderRequestCaptor = ArgumentCaptor.forClass(CheckoutOrderRequest.class);
        verify(orderCheckoutApi).createPendingPaymentOrderSnapshot(eq(USER_ID), orderRequestCaptor.capture(), eq(cart));
        assertThat(orderRequestCaptor.getValue().recipientName()).isEqualTo("John");
        assertThat(orderRequestCaptor.getValue().recipientSurname()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("prepareCheckout returns existing order/payment on idempotent retry")
    void prepareCheckout_idempotentHit_returnsExisting() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment existingPayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(USER_ID)
                .providerSessionId("cs_test_existing")
                .status(PaymentStatus.STRIPE_SESSION_CREATED)
                .build();
        OrderSnapshot existingOrder = new OrderSnapshot(
                orderId,
                USER_ID,
                OrderStatusSnapshot.PENDING_PAYMENT,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.of(existingPayment));
        when(orderPaymentApi.getSnapshot(orderId)).thenReturn(existingOrder);

        CheckoutPreparation result = service.prepareCheckout(
                USER_ID, new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B"), IDEMPOTENCY_KEY);

        assertThat(result).isInstanceOf(CheckoutPreparation.ExistingCheckout.class);
        assertThat(result.order()).isEqualTo(existingOrder);
        assertThat(result.payment()).isEqualTo(new CheckoutPaymentSnapshot(paymentId, "cs_test_existing"));
        verify(shoppingCartService, never()).getByUserIdOrThrow(any());
    }

    @Test
    @DisplayName("prepareCheckout uses fetch join for items when providerSessionId is null (retry before Stripe call)")
    void prepareCheckout_idempotentHit_noSessionId_usesFetchJoin() {
        UUID orderId = UUID.randomUUID();
        Payment existingPayment = Payment.builder()
                .orderId(orderId)
                .userId(USER_ID)
                .providerSessionId(null)
                .status(PaymentStatus.CREATED)
                .build();
        OrderSnapshot existingOrder = new OrderSnapshot(
                orderId,
                USER_ID,
                OrderStatusSnapshot.PENDING_PAYMENT,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.of(existingPayment));
        when(orderPaymentApi.getSnapshotWithItems(orderId)).thenReturn(existingOrder);

        CheckoutPreparation result = service.prepareCheckout(
                USER_ID, new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B"), IDEMPOTENCY_KEY);

        assertThat(result).isInstanceOf(CheckoutPreparation.ExistingCheckout.class);
        verify(orderPaymentApi).getSnapshotWithItems(orderId);
        verify(orderPaymentApi, never()).getSnapshot(orderId);
    }

    @Test
    @DisplayName("prepareCheckout throws BadRequestException for empty cart")
    void prepareCheckout_emptyCart_throws() {
        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.empty());
        when(shoppingCartService.getByUserIdOrThrow(USER_ID))
                .thenReturn(new CartSnapshot(
                        UUID.randomUUID(), USER_ID, List.of(), 0, BigDecimal.ZERO, 0, OffsetDateTime.now(), null));

        assertThatThrownBy(() -> service.prepareCheckout(
                        USER_ID,
                        new CreateCheckoutRequestDto().recipientName("A").recipientSurname("B"),
                        IDEMPOTENCY_KEY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("prepareCheckout rejects inline address with missing required fields")
    void prepareCheckout_incompleteInlineAddress_throws() {
        CreateCheckoutRequestDto request = new CreateCheckoutRequestDto()
                .recipientName("A")
                .recipientSurname("B")
                .address(new AddressDto().city("London").line("123 Coffee St").postcode("E1 6AN"));

        var productInfo = new ProductSnapshot(
                UUID.randomUUID(),
                "Coffee",
                "Desc",
                BigDecimal.valueOf(12.50),
                10,
                true,
                null,
                BigDecimal.valueOf(4.5),
                12,
                "Brand",
                "Seller",
                250);
        var cartItem = new CartItemSnapshot(UUID.randomUUID(), productInfo, 1);
        CartSnapshot cart = new CartSnapshot(
                UUID.randomUUID(),
                USER_ID,
                List.of(cartItem),
                1,
                BigDecimal.valueOf(12.50),
                1,
                OffsetDateTime.now(),
                null);

        when(paymentRepository.findByCheckoutIdempotencyKeyAndUserId(IDEMPOTENCY_KEY, USER_ID))
                .thenReturn(Optional.empty());
        when(shoppingCartService.getByUserIdOrThrow(USER_ID)).thenReturn(cart);

        assertThatThrownBy(() -> service.prepareCheckout(USER_ID, request, IDEMPOTENCY_KEY))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("country");

        verify(orderCheckoutApi, never()).createPendingPaymentOrderSnapshot(any(), any(), any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("saveStripeDetails updates payment with session ID and status")
    void saveStripeDetails_updatesPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment =
                Payment.builder().id(paymentId).status(PaymentStatus.CREATED).build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveStripeDetails(paymentId, new StripeSessionResult("cs_test_123", "https://checkout.stripe.com/..."));

        assertThat(payment.getProviderSessionId()).isEqualTo("cs_test_123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.STRIPE_SESSION_CREATED);
        verify(paymentRepository).save(payment);
    }
}
