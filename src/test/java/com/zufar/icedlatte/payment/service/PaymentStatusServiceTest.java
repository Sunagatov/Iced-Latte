package com.zufar.icedlatte.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.openapi.dto.CheckoutStatusDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.checkout.StripeSessionGateway;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentStatusService unit tests")
class PaymentStatusServiceTest {

    @Mock
    private OrderPaymentApi orderPaymentApi;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CartCheckoutApi shoppingCartService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private StripeSessionGateway stripeSessionGateway;

    @InjectMocks
    private PaymentStatusService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Returns PAID status for completed payment")
    void getStatus_paid_returnsPaidStatus() {
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                USER_ID,
                com.zufar.icedlatte.order.api.OrderStatusSnapshot.PAID,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());
        Payment payment =
                Payment.builder().orderId(ORDER_ID).status(PaymentStatus.PAID).build();

        when(orderPaymentApi.getSnapshot(ORDER_ID)).thenReturn(order);
        when(currentUserProvider.get()).thenReturn(currentUser());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getPaymentStatus()).isEqualTo(CheckoutStatusDto.PaymentStatusEnum.PAID);
    }

    @Test
    @DisplayName("Returns PENDING_PAYMENT status for in-progress order without session ID (no Stripe sync)")
    void getStatus_pending_noSessionId_returnsPendingStatus() {
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                USER_ID,
                com.zufar.icedlatte.order.api.OrderStatusSnapshot.PENDING_PAYMENT,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());
        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .providerSessionId(null)
                .status(PaymentStatus.CREATED)
                .build();

        when(orderPaymentApi.getSnapshot(ORDER_ID)).thenReturn(order);
        when(currentUserProvider.get()).thenReturn(currentUser());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getPaymentStatus()).isEqualTo(CheckoutStatusDto.PaymentStatusEnum.CREATED);
    }

    @Test
    @DisplayName("Throws OrderNotFoundException for unknown order")
    void getStatus_unknownOrder_throws() {
        when(orderPaymentApi.getSnapshot(ORDER_ID))
                .thenThrow(new com.zufar.icedlatte.order.exception.OrderNotFoundException(ORDER_ID));

        assertThatThrownBy(() -> service.getStatus(ORDER_ID)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Throws OrderAccessDeniedException for other user's order")
    void getStatus_otherUser_throws() {
        UUID otherUserId = UUID.randomUUID();
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                otherUserId,
                com.zufar.icedlatte.order.api.OrderStatusSnapshot.PAID,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());

        when(orderPaymentApi.getSnapshot(ORDER_ID)).thenReturn(order);
        when(currentUserProvider.get()).thenReturn(currentUser());

        assertThatThrownBy(() -> service.getStatus(ORDER_ID)).isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    @DisplayName("Returns status without paymentStatus when no Payment entity exists")
    void getStatus_noPayment_returnsOrderStatusOnly() {
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                USER_ID,
                com.zufar.icedlatte.order.api.OrderStatusSnapshot.PENDING_PAYMENT,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());

        when(orderPaymentApi.getSnapshot(ORDER_ID)).thenReturn(order);
        when(currentUserProvider.get()).thenReturn(currentUser());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getPaymentStatus()).isNull();
    }

    @Test
    @DisplayName("Stripe sync fails closed when paid session lacks reconciliation fields")
    void getStatus_paidSessionMissingAmount_setsReconciliationFailed() throws Exception {
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                USER_ID,
                com.zufar.icedlatte.order.api.OrderStatusSnapshot.PENDING_PAYMENT,
                java.math.BigDecimal.TEN,
                null,
                java.util.List.of());
        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .providerSessionId("cs_test_1")
                .amountMinor(1000L)
                .currency("usd")
                .status(PaymentStatus.STRIPE_SESSION_CREATED)
                .build();
        Session session = mock(Session.class);

        when(orderPaymentApi.getSnapshot(ORDER_ID)).thenReturn(order);
        when(currentUserProvider.get()).thenReturn(currentUser());
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(stripeSessionGateway.retrieve("cs_test_1")).thenReturn(session);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(null);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getPaymentIntent()).thenReturn("pi_test");
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        doAnswer(invocation -> {
                    java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action =
                            invocation.getArgument(0);
                    action.accept(new SimpleTransactionStatus());
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        assertThat(result.getPaymentStatus()).isEqualTo(CheckoutStatusDto.PaymentStatusEnum.RECONCILIATION_FAILED);
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
        verify(shoppingCartService, never()).deleteCartForUser(any());
    }

    private static CurrentUserSnapshot currentUser() {
        return new CurrentUserSnapshot(USER_ID, "user@example.com");
    }
}
