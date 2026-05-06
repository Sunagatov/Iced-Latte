package com.zufar.icedlatte.email.sender;

import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PaymentEmailConfirmation")
class PaymentEmailConfirmationTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    private final SimpleMailMessage mailMessage = new SimpleMailMessage();

    private static MessageSource testMessageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages/messages");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    @Test
    @DisplayName("sends formatted payment confirmation email from stripe session")
    void sendsFormattedPaymentConfirmation() {
        Session stripeSession = mock(Session.class);
        when(stripeSession.getAmountTotal()).thenReturn(12345L);
        when(stripeSession.getCurrency()).thenReturn("usd");
        when(stripeSession.getCustomerEmail()).thenReturn("buyer@example.com");

        PaymentEmailConfirmation confirmation =
                new PaymentEmailConfirmation(javaMailSender, mailMessage, List.of(), testMessageSource());

        confirmation.send(stripeSession);

        assertThat(mailMessage.getTo()).containsExactly("buyer@example.com");
        assertThat(mailMessage.getSubject()).isEqualTo("Payment Confirmation for Your Recent Purchase");
        assertThat(mailMessage.getText()).isEqualTo(
                "Your payment with total amount - 123.45 usd was successfully processed"
        );
        verify(javaMailSender).send(mailMessage);
    }
}
