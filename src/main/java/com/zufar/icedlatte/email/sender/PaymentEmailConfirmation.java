package com.zufar.icedlatte.email.sender;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import com.zufar.icedlatte.email.message.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
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
        sendNotification(
                stripeSession.getCustomerEmail(),
                DEFAULT_SUCCESSFUL_EMAIL_MESSAGE.formatted(stripeSession.getAmountTotal() / 100.0, stripeSession.getCurrency()),
                DEFAULT_EMAIL_SUBJECT
        );
    }
}
