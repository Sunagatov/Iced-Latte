package com.zufar.icedlatte.email.sender;

import com.stripe.model.PaymentIntent;
import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import com.zufar.icedlatte.email.message.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentEmailConfirmation extends AbstractEmailSender<EmailConfirmMessage> {


    private static final String DEFAULT_SUCCESSFUL_EMAIL_MESSAGE = "You're payment with total amount - %d %s was successfully processed";

    private static final String DEFAULT_EMAIL_SUBJECT = "Payment Confirmation for Your Recent Purchase";

    @Autowired
    public PaymentEmailConfirmation(JavaMailSender javaMailSender,
                                    SimpleMailMessage mailMessage,
                                    List<MessageBuilder<EmailConfirmMessage>> messageBuilders) {
        super(javaMailSender, mailMessage, messageBuilders);
    }

    public void send(PaymentIntent paymentIntent) {
        sendNotification(
                paymentIntent.getReceiptEmail(),
                DEFAULT_SUCCESSFUL_EMAIL_MESSAGE.formatted(paymentIntent.getAmount(), paymentIntent.getCurrency()),
                DEFAULT_EMAIL_SUBJECT
        );
    }
}
