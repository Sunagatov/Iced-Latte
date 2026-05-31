package com.zufar.icedlatte.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.checkout.StripeSessionGateway;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationService unit tests")
class PaymentReconciliationServiceTest {

    @Mock
    private OrderPaymentApi orderPaymentApi;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CartCheckoutApi cartCheckoutApi;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private StripeSessionGateway stripeSessionGateway;

    @InjectMocks
    private PaymentReconciliationService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Paid Stripe session updates payment, order, and cart")
    void trySyncPaidStatus_paidSession_updatesLocalState() throws Exception {
        Payment payment = payment();
        Session session = paidSession(1000L);

        when(stripeSessionGateway.retrieve("cs_test_1")).thenReturn(session);
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderPaymentApi.confirmPayment(ORDER_ID, "Stripe payment confirmed (sync fallback)"))
                .thenReturn(true);
        executeTransactionTemplate();

        service.trySyncPaidStatus(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderPaymentIntentId()).isEqualTo("pi_test");
        assertThat(payment.getLatestEventType()).isEqualTo("sync.session.retrieve");
        verify(orderPaymentApi).assignPaymentIntent(ORDER_ID, "pi_test");
        verify(cartCheckoutApi).deleteCartForUser(USER_ID);
    }

    @Test
    @DisplayName("Paid Stripe session without reconciliation fields fails closed")
    void trySyncPaidStatus_paidSessionMissingAmount_setsReconciliationFailed() throws Exception {
        Payment payment = payment();
        Session session = paidSession(null);

        when(stripeSessionGateway.retrieve("cs_test_1")).thenReturn(session);
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        executeTransactionTemplate();

        service.trySyncPaidStatus(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        assertThat(payment.getLatestEventType()).isEqualTo("sync.session.retrieve");
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
        verify(cartCheckoutApi, never()).deleteCartForUser(any());
    }

    @Test
    @DisplayName("Unpaid Stripe session does not update local state")
    void trySyncPaidStatus_unpaidSession_doesNothing() throws Exception {
        Payment payment = payment();
        Session session = mock(Session.class);

        when(stripeSessionGateway.retrieve("cs_test_1")).thenReturn(session);
        when(session.getPaymentStatus()).thenReturn("unpaid");

        service.trySyncPaidStatus(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.STRIPE_SESSION_CREATED);
        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    private static Payment payment() {
        return Payment.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .providerSessionId("cs_test_1")
                .amountMinor(1000L)
                .currency("usd")
                .status(PaymentStatus.STRIPE_SESSION_CREATED)
                .build();
    }

    private static Session paidSession(Long amountTotal) {
        Session session = mock(Session.class);
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(amountTotal);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getPaymentIntent()).thenReturn("pi_test");
        return session;
    }

    private void executeTransactionTemplate() {
        doAnswer(invocation -> {
                    java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action =
                            invocation.getArgument(0);
                    action.accept(new SimpleTransactionStatus());
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
    }
}
