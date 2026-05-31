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

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentConfirmationService unit tests")
class PaymentConfirmationServiceTest {

    @Mock
    private OrderPaymentApi orderPaymentApi;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CartCheckoutApi cartCheckoutApi;

    @InjectMocks
    private PaymentConfirmationService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final PaymentConfirmationSource WEBHOOK_SOURCE =
            new PaymentConfirmationSource("evt_1", "checkout.session.completed", "Stripe payment confirmed");

    @Test
    @DisplayName("valid paid Stripe session marks payment paid and transitions order")
    void confirmPaid_validSession_marksPaid() {
        Payment payment = payment(PaymentStatus.STRIPE_SESSION_CREATED);
        Session session = session(1000L, "pi_test");

        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderPaymentApi.confirmPayment(ORDER_ID, "Stripe payment confirmed"))
                .thenReturn(true);

        service.confirmPaid(ORDER_ID, session, WEBHOOK_SOURCE);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderPaymentIntentId()).isEqualTo("pi_test");
        assertThat(payment.getRawEventId()).isEqualTo("evt_1");
        assertThat(payment.getLatestEventType()).isEqualTo("checkout.session.completed");
        verify(orderPaymentApi).assignPaymentIntent(ORDER_ID, "pi_test");
        verify(cartCheckoutApi).deleteCartForUser(USER_ID);
    }

    @Test
    @DisplayName("amount mismatch fails closed")
    void confirmPaid_amountMismatch_setsReconciliationFailed() {
        Payment payment = payment(PaymentStatus.STRIPE_SESSION_CREATED);
        Session session = session(9999L, "pi_test");

        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        service.confirmPaid(ORDER_ID, session, WEBHOOK_SOURCE);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        assertThat(payment.getRawEventId()).isEqualTo("evt_1");
        assertThat(payment.getLatestEventType()).isEqualTo("checkout.session.completed");
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
        verify(cartCheckoutApi, never()).deleteCartForUser(any());
    }

    @Test
    @DisplayName("missing payment intent fails closed")
    void confirmPaid_missingPaymentIntent_setsReconciliationFailed() {
        Payment payment = payment(PaymentStatus.STRIPE_SESSION_CREATED);
        Session session = session(1000L, null);

        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        service.confirmPaid(ORDER_ID, session, WEBHOOK_SOURCE);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
    }

    @Test
    @DisplayName("rejected order transition fails closed without assigning payment intent")
    void confirmPaid_orderTransitionRejected_setsReconciliationFailed() {
        Payment payment = payment(PaymentStatus.STRIPE_SESSION_CREATED);
        Session session = session(1000L, "pi_test");

        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderPaymentApi.confirmPayment(ORDER_ID, "Stripe payment confirmed"))
                .thenReturn(false);

        service.confirmPaid(ORDER_ID, session, WEBHOOK_SOURCE);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        assertThat(payment.getProviderPaymentIntentId()).isEqualTo("pi_test");
        verify(orderPaymentApi, never()).assignPaymentIntent(any(), any());
        verify(cartCheckoutApi, never()).deleteCartForUser(any());
    }

    @Test
    @DisplayName("terminal payments are idempotently skipped")
    void confirmPaid_terminalPayment_skips() {
        Payment payment = payment(PaymentStatus.PAID);
        Session session = mock(Session.class);

        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        service.confirmPaid(ORDER_ID, session, WEBHOOK_SOURCE);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any());
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
    }

    private static Payment payment(PaymentStatus status) {
        return Payment.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .amountMinor(1000L)
                .currency("usd")
                .status(status)
                .build();
    }

    private static Session session(Long amountTotal, String paymentIntent) {
        Session session = mock(Session.class);
        when(session.getAmountTotal()).thenReturn(amountTotal);
        when(session.getCurrency()).thenReturn("usd");
        when(session.getPaymentIntent()).thenReturn(paymentIntent);
        return session;
    }
}
