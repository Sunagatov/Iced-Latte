package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.CheckoutStatusDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.order.api.OrderStatusTransitioner;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.api.OrderDetailProvider;
import com.zufar.icedlatte.order.api.OrderLifecycleService;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentStatusService unit tests")
class PaymentStatusServiceTest {

    @Mock private OrderDetailProvider orderDetailProvider;
    @Mock private OrderLifecycleService orderLifecycleService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderStatusTransitioner orderStatusTransitioner;
    @Mock private ShoppingCartRepository shoppingCartRepository;
    @Mock private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock private TransactionTemplate transactionTemplate;
    @InjectMocks private PaymentStatusService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Returns PAID status for completed payment")
    void getStatus_paid_returnsPaidStatus() {
        Order order = Order.builder().id(ORDER_ID).userId(USER_ID).status(OrderStatus.PAID).build();
        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.PAID).build();

        when(orderDetailProvider.findById(ORDER_ID)).thenReturn(order);
        when(securityPrincipalProvider.get()).thenReturn(new UserDto().id(USER_ID));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getPaymentStatus()).isEqualTo(CheckoutStatusDto.PaymentStatusEnum.PAID);
    }

    @Test
    @DisplayName("Returns PENDING_PAYMENT status for in-progress order without session ID (no Stripe sync)")
    void getStatus_pending_noSessionId_returnsPendingStatus() {
        Order order = Order.builder().id(ORDER_ID).userId(USER_ID)
                .status(OrderStatus.PENDING_PAYMENT).build();
        Payment payment = Payment.builder().orderId(ORDER_ID)
                .providerSessionId(null)
                .status(PaymentStatus.CREATED).build();

        when(orderDetailProvider.findById(ORDER_ID)).thenReturn(order);
        when(securityPrincipalProvider.get()).thenReturn(new UserDto().id(USER_ID));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getPaymentStatus()).isEqualTo(CheckoutStatusDto.PaymentStatusEnum.CREATED);
    }

    @Test
    @DisplayName("Throws OrderNotFoundException for unknown order")
    void getStatus_unknownOrder_throws() {
        when(orderDetailProvider.findById(ORDER_ID)).thenThrow(new com.zufar.icedlatte.order.exception.OrderNotFoundException(ORDER_ID));

        assertThatThrownBy(() -> service.getStatus(ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Throws OrderAccessDeniedException for other user's order")
    void getStatus_otherUser_throws() {
        UUID otherUserId = UUID.randomUUID();
        Order order = Order.builder().id(ORDER_ID).userId(otherUserId).status(OrderStatus.PAID).build();

        when(orderDetailProvider.findById(ORDER_ID)).thenReturn(order);
        when(securityPrincipalProvider.get()).thenReturn(new UserDto().id(USER_ID));

        assertThatThrownBy(() -> service.getStatus(ORDER_ID))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    @DisplayName("Returns status without paymentStatus when no Payment entity exists")
    void getStatus_noPayment_returnsOrderStatusOnly() {
        Order order = Order.builder().id(ORDER_ID).userId(USER_ID)
                .status(OrderStatus.PENDING_PAYMENT).build();

        when(orderDetailProvider.findById(ORDER_ID)).thenReturn(order);
        when(securityPrincipalProvider.get()).thenReturn(new UserDto().id(USER_ID));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        CheckoutStatusDto result = service.getStatus(ORDER_ID);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getPaymentStatus()).isNull();
    }
}
