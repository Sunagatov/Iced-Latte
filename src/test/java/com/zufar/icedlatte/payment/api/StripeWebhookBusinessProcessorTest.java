package com.zufar.icedlatte.payment.api;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderStatusTransitioner;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookBusinessProcessor unit tests")
class StripeWebhookBusinessProcessorTest {

    @Mock private OrderStatusTransitioner orderStatusTransitioner;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ShoppingCartRepository shoppingCartRepository;
    @InjectMocks private StripeWebhookBusinessProcessor processor;

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("checkout.session.completed with paid status marks payment PAID and transitions order")
    void handleSessionCompleted_paid_marksPaidAndTransitions() {
        Event event = mockEvent("checkout.session.completed", "evt_1");
        Session session = mockSession("paid", 2500L, "usd", "pi_test_123");
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).amountMinor(2500L)
                .currency("usd").status(PaymentStatus.STRIPE_SESSION_CREATED).build();
        Order order = Order.builder().id(ORDER_ID).userId(USER_ID)
                .status(OrderStatus.PENDING_PAYMENT).build();

        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(orderStatusTransitioner.transition(eq(ORDER_ID), eq(OrderEvent.PENDING_PAYMENT_CONFIRMED),
                any(), any())).thenReturn(order);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderPaymentIntentId()).isEqualTo("pi_test_123");
        verify(orderStatusTransitioner).transition(eq(ORDER_ID), eq(OrderEvent.PENDING_PAYMENT_CONFIRMED),
                any(), eq("Stripe payment confirmed"));
        verify(shoppingCartRepository).deleteByUserId(USER_ID);
    }

    @Test
    @DisplayName("checkout.session.completed with unpaid status sets AWAITING_ASYNC_CONFIRMATION")
    void handleSessionCompleted_unpaid_setsAwaitingAsync() {
        Event event = mockEvent("checkout.session.completed", "evt_2");
        Session session = mockSession("unpaid", null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.STRIPE_SESSION_CREATED).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AWAITING_ASYNC_CONFIRMATION);
        verify(orderStatusTransitioner, never()).transition(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Already PAID payment is idempotent — no duplicate transition")
    void markPaid_alreadyPaid_idempotent() {
        Event event = mockEvent("checkout.session.completed", "evt_3");
        Session session = mockSession("paid", 2500L, "usd", "pi_test_123");
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).amountMinor(2500L)
                .currency("usd").status(PaymentStatus.PAID).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        verify(orderStatusTransitioner, never()).transition(any(), any(), any(), any());
        verify(shoppingCartRepository, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("Amount mismatch sets RECONCILIATION_FAILED and returns normally (no rollback)")
    void markPaid_amountMismatch_setsReconciliationFailed() {
        Event event = mockEvent("checkout.session.completed", "evt_4");
        Session session = mockSession("paid", 9999L, "usd", "pi_test_123");
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).amountMinor(2500L)
                .currency("usd").status(PaymentStatus.STRIPE_SESSION_CREATED).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Should NOT throw — returns normally so @Transactional commits
        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
        verify(orderStatusTransitioner, never()).transition(any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkout.session.expired marks payment EXPIRED and transitions order")
    void handleExpired_marksExpiredAndTransitions() {
        Event event = mockEvent("checkout.session.expired", "evt_5");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.STRIPE_SESSION_CREATED).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verify(orderStatusTransitioner).transition(eq(ORDER_ID), eq(OrderEvent.PAYMENT_EXPIRED_EVENT),
                any(), eq("Stripe session expired"));
    }

    @Test
    @DisplayName("checkout.session.async_payment_failed marks payment FAILED")
    void handleAsyncPaymentFailed_marksFailed() {
        Event event = mockEvent("checkout.session.async_payment_failed", "evt_6");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.AWAITING_ASYNC_CONFIRMATION).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderStatusTransitioner).transition(eq(ORDER_ID), eq(OrderEvent.PAYMENT_FAILED_EVENT),
                any(), eq("Stripe async payment failed"));
    }

    @Test
    @DisplayName("checkout.session.expired does not overwrite PAID payment")
    void handleExpired_paidPayment_skipped() {
        Event event = mockEvent("checkout.session.expired", "evt_7");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.PAID).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any());
        verify(orderStatusTransitioner, never()).transition(any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkout.session.async_payment_failed does not overwrite PAID payment")
    void handleAsyncPaymentFailed_paidPayment_skipped() {
        Event event = mockEvent("checkout.session.async_payment_failed", "evt_8");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.PAID).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any());
        verify(orderStatusTransitioner, never()).transition(any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkout.session.expired does not overwrite FAILED payment")
    void handleExpired_failedPayment_skipped() {
        Event event = mockEvent("checkout.session.expired", "evt_9");
        Session session = mockSession(null, null, null, null);
        mockEventSession(event, session);

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.FAILED).build();
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

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.EXPIRED).build();
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

        Payment payment = Payment.builder().orderId(ORDER_ID).status(PaymentStatus.RECONCILIATION_FAILED).build();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        processor.process(event);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_FAILED);
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
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));
    }
}
