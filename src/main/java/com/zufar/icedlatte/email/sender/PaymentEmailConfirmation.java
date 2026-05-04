package com.zufar.icedlatte.email.sender;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import com.zufar.icedlatte.email.message.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "email.enabled", havingValue = "true")
public class PaymentEmailConfirmation extends AbstractEmailSender<EmailConfirmMessage> {

    private static final String DEFAULT_SUCCESSFUL_EMAIL_MESSAGE = "Your payment with total amount - %.2f %s was successfully processed";
    private static final String DEFAULT_EMAIL_SUBJECT = "Payment Confirmation for Your Recent Purchase";

    @Autowired
    public PaymentEmailConfirmation(JavaMailSender javaMailSender,
                                    SimpleMailMessage mailMessage,
                                    List<MessageBuilder<EmailConfirmMessage>> messageBuilders) {
        super(javaMailSender, mailMessage, messageBuilders);
    }

    public void send(Session stripeSession) {
        double value = stripeSession.getAmountTotal() / 100.0;
        String currency = stripeSession.getCurrency();

        sendNotification(stripeSession.getCustomerEmail(),
                DEFAULT_SUCCESSFUL_EMAIL_MESSAGE.formatted(value, currency),
                DEFAULT_EMAIL_SUBJECT
        );
    }
}
