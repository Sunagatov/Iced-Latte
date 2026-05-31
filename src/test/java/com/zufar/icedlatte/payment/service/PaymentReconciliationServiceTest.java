package com.zufar.icedlatte.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.service.checkout.StripeSessionGateway;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationService unit tests")
class PaymentReconciliationServiceTest {

    @Mock
    private StripeSessionGateway stripeSessionGateway;

    @Mock
    private PaymentConfirmationService paymentConfirmationService;

    @InjectMocks
    private PaymentReconciliationService service;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Paid Stripe session delegates to shared confirmation workflow")
    void trySyncPaidStatus_paidSession_delegatesConfirmation() throws Exception {
        Payment payment = payment();
        Session session = mock(Session.class);

        when(stripeSessionGateway.retrieve("cs_test_1")).thenReturn(session);
        when(session.getPaymentStatus()).thenReturn("paid");

        service.trySyncPaidStatus(payment);

        verify(paymentConfirmationService)
                .confirmPaid(
                        ORDER_ID,
                        session,
                        new PaymentConfirmationSource(
                                null, "sync.session.retrieve", "Stripe payment confirmed (sync fallback)"));
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
        verifyNoInteractions(paymentConfirmationService);
    }

    @Test
    @DisplayName("Stripe retrieve error is swallowed so status polling still returns")
    void trySyncPaidStatus_stripeError_doesNothing() throws Exception {
        Payment payment = payment();

        when(stripeSessionGateway.retrieve("cs_test_1")).thenThrow(mock(com.stripe.exception.StripeException.class));

        service.trySyncPaidStatus(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.STRIPE_SESSION_CREATED);
        verifyNoInteractions(paymentConfirmationService);
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

}
