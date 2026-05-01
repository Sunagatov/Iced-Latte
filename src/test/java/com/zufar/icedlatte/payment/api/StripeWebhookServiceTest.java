package com.zufar.icedlatte.payment.api;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zufar.icedlatte.email.sender.PaymentEmailConfirmation;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookService unit tests")
class StripeWebhookServiceTest {

    @Mock
    @SuppressWarnings("unused")
    private OrderCreator orderCreator;
    @Mock
    @SuppressWarnings("unused")
    private PaymentEmailConfirmation paymentEmailConfirmation;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    @AfterEach
    void detachAppenders() {
        Logger logger = (Logger) LoggerFactory.getLogger(StripeWebhookService.class);
        logger.detachAndStopAllAppenders();
    }

    @Test
    @DisplayName("invalid signature uses safe exception message and safe warning log")
    void invalidSignatureUsesSafeMessageAndSafeLog() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", "whsec_test");
        String signature = "t=123,v1=secret-signature";

        assertThatThrownBy(() -> stripeWebhookService.processWebhook("{\"id\":\"evt_1\"}", signature))
                .isInstanceOf(PaymentEventProcessingException.class)
                .hasMessage("Stripe webhook signature verification failed.")
                .hasMessageNotContaining(signature);

        assertThat(appender.list)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).isEqualTo("payment.webhook.signature_invalid");
                    assertThat(event.getFormattedMessage()).doesNotContain(signature);
                });
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(StripeWebhookService.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
