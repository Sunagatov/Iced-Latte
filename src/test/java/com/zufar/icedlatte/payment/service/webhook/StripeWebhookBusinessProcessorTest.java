package com.zufar.icedlatte.payment.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.PaymentConfirmationService;
import com.zufar.icedlatte.payment.service.PaymentConfirmationSource;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookBusinessProcessor unit tests")
class StripeWebhookBusinessProcessorTest {

    @Mock
    private OrderPaymentApi orderPaymentApi;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentConfirmationService paymentConfirmationService;

    private StripeWebhookBusinessProcessor processor;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        processor = new StripeWebhookBusinessProcessor(List.of(
                new CheckoutSessionCompletedWebhookHandler(paymentRepository, paymentConfirmationService),
                new CheckoutSessionExpiredWebhookHandler(orderPaymentApi, paymentRepository),
                new CheckoutSessionAsyncPaymentFailedWebhookHandler(orderPaymentApi, paymentRepository),
                new ChargeRefundedWebhookHandler(orderPaymentApi, paymentRepository)));
    }

    @Test
    @DisplayName("checkout.session.completed with paid status marks payment PAID and transitions order")
    void handleSessionCompleted_paid_marksPaidAndTransitions() {
        Event event = mockEvent("checkout.session.completed", "evt_1");
        Session session = mockSession("paid", 2500L, "usd", "pi_test_123");
        mockEventSession(event, session);

        processor.process(event);

        PaymentConfirmationSource paymentConfirmationSource =
                new PaymentConfirmationSource("evt_1", "checkout.session.completed", "Stripe payment confirmed");
        verify(paymentConfirmationService).confirmPaid(eq(ORDER_ID), eq(session), eq(paymentConfirmationSource));
    }

    @Test
    @DisplayName("checkout.session.completed with unpaid status sets AWAITING_ASYNC_CONFIRMATION")
    void handleSessionCompleted_unpaid_setsAwaitingAsync() {
        Event event = mockEvent("checkout.session.completed", "evt_2");
        Session session = mockSession("unpaid", null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .status(PaymentStatus.STRIPE_SESSION_CREATED)
                .build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AWAITING_ASYNC_CONFIRMATION);
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
    }

    @Test
    @DisplayName("Already PAID payment is idempotent — no duplicate transition")
    void markPaid_alreadyPaid_idempotent() {
        Event event = mockEvent("checkout.session.completed", "evt_3");
        Session session = mockSession("paid", 2500L, "usd", "pi_test_123");
        mockEventSession(event, session);

        processor.process(event);

        verify(paymentConfirmationService)
                .confirmPaid(
                        eq(ORDER_ID),
                        eq(session),
                        eq(new PaymentConfirmationSource(
                                "evt_3", "checkout.session.completed", "Stripe payment confirmed")));
    }

    @Test
    @DisplayName("checkout.session.expired marks payment EXPIRED and transitions order")
    void handleExpired_marksExpiredAndTransitions() {
        Event event = mockEvent("checkout.session.expired", "evt_5");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .status(PaymentStatus.STRIPE_SESSION_CREATED)
                .build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(orderPaymentApi).expirePayment(eq(ORDER_ID), eq("Stripe session expired"));
    }

    @Test
    @DisplayName("checkout.session.async_payment_failed marks payment FAILED")
    void handleAsyncPaymentFailed_marksFailed() {
        Event event = mockEvent("checkout.session.async_payment_failed", "evt_6");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .status(PaymentStatus.AWAITING_ASYNC_CONFIRMATION)
                .build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderPaymentApi).failPayment(eq(ORDER_ID), eq("Stripe async payment failed"));
    }

    @Test
    @DisplayName("checkout.session.expired does not overwrite PAID payment")
    void handleExpired_paidPayment_skipped() {
        Event event = mockEvent("checkout.session.expired", "evt_7");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment =
                Payment.builder().orderId(ORDER_ID).status(PaymentStatus.PAID).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any());
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
    }

    @Test
    @DisplayName("checkout.session.async_payment_failed does not overwrite PAID payment")
    void handleAsyncPaymentFailed_paidPayment_skipped() {
        Event event = mockEvent("checkout.session.async_payment_failed", "evt_8");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment =
                Payment.builder().orderId(ORDER_ID).status(PaymentStatus.PAID).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any());
        verify(orderPaymentApi, never()).confirmPayment(any(), any());
    }

    @Test
    @DisplayName("checkout.session.expired does not overwrite FAILED payment")
    void handleExpired_failedPayment_skipped() {
        Event event = mockEvent("checkout.session.expired", "evt_9");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment =
                Payment.builder().orderId(ORDER_ID).status(PaymentStatus.FAILED).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout.session.async_payment_failed does not overwrite EXPIRED payment")
    void handleAsyncPaymentFailed_expiredPayment_skipped() {
        Event event = mockEvent("checkout.session.async_payment_failed", "evt_10");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .status(PaymentStatus.EXPIRED)
                .build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("checkout.session.completed awaiting-async skips RECONCILIATION_FAILED payment")
    void handleSessionCompleted_unpaid_reconciliationFailed_skipped() {
        Event event = mockEvent("checkout.session.completed", "evt_11");
        Session session = mockSession("unpaid", null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .status(PaymentStatus.RECONCILIATION_FAILED)
                .build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("charge.refunded confirms refund and marks payment REFUNDED")
    void chargeRefunded_refundRequested_marksPaymentRefunded() {
        Event event = mockEvent("charge.refunded", "evt_refund");
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn("pi_refund");
        when(charge.getAmount()).thenReturn(1000L);
        when(charge.getAmountRefunded()).thenReturn(1000L);
        mockEventObject(event, charge);
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                USER_ID,
                OrderStatusSnapshot.REFUND_REQUESTED,
                java.math.BigDecimal.TEN,
                "pi_refund",
                List.of());
        Payment payment = Payment.builder()
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .providerPaymentIntentId("pi_refund")
                .status(PaymentStatus.PAID)
                .build();

        when(orderPaymentApi.findByStripePaymentIntentId("pi_refund")).thenReturn(Optional.of(order));
        when(orderPaymentApi.confirmRefund(ORDER_ID, "Stripe refund confirmed")).thenReturn(true);
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        verify(orderPaymentApi).confirmRefund(ORDER_ID, "Stripe refund confirmed");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRawEventId()).isEqualTo("evt_refund");
        assertThat(payment.getLatestEventType()).isEqualTo("charge.refunded");
    }

    @Test
    @DisplayName("charge.refunded ignores partial refunds")
    void chargeRefunded_partialRefund_ignoresOrderTransition() {
        Event event = mockEvent("charge.refunded", "evt_partial_refund");
        Charge charge = mock(Charge.class);
        when(charge.getPaymentIntent()).thenReturn("pi_refund");
        when(charge.getAmount()).thenReturn(1000L);
        when(charge.getAmountRefunded()).thenReturn(400L);
        mockEventObject(event, charge);
        OrderSnapshot order = new OrderSnapshot(
                ORDER_ID,
                USER_ID,
                OrderStatusSnapshot.REFUND_REQUESTED,
                java.math.BigDecimal.TEN,
                "pi_refund",
                List.of());

        when(orderPaymentApi.findByStripePaymentIntentId("pi_refund")).thenReturn(Optional.of(order));

        processor.process(event);

        verify(orderPaymentApi, never()).confirmRefund(any(), any());
        verify(paymentRepository, never()).findByOrderIdForUpdate(any());
        verify(paymentRepository, never()).save(any());
    }

    // --- Helpers ---

    private Event mockEvent(String type, String eventId) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        lenient().when(event.getId()).thenReturn(eventId);
        return event;
    }

    private Session mockSession(String paymentStatus, Long amountTotal, String currency, String paymentIntent) {
        Session session = mock(Session.class);
        when(session.getClientReferenceId()).thenReturn(ORDER_ID.toString());
        lenient().when(session.getMetadata()).thenReturn(Map.of("orderId", ORDER_ID.toString()));
        lenient().when(session.getPaymentStatus()).thenReturn(paymentStatus);
        lenient().when(session.getAmountTotal()).thenReturn(amountTotal);
        lenient().when(session.getCurrency()).thenReturn(currency);
        lenient().when(session.getPaymentIntent()).thenReturn(paymentIntent);
        return session;
    }

    private void mockEventSession(Event event, Session session) {
        mockEventObject(event, session);
    }

    private void mockEventObject(Event event, com.stripe.model.StripeObject object) {
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(object));
    }
}
