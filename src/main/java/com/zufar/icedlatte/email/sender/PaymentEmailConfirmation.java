package com.zufar.icedlatte.email.sender;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import com.zufar.icedlatte.email.message.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "email.enabled", havingValue = "true")
public class PaymentEmailConfirmation extends AbstractEmailSender<EmailConfirmMessage> {

    private final MessageSource messageSource;

    @Autowired
    public PaymentEmailConfirmation(JavaMailSender javaMailSender,
                                    SimpleMailMessage mailMessage,
                                    List<MessageBuilder<EmailConfirmMessage>> messageBuilders,
                                    MessageSource messageSource) {
        super(javaMailSender, mailMessage, messageBuilders);
        this.messageSource = messageSource;
    }

    public void send(Session stripeSession) {
        double value = stripeSession.getAmountTotal() / 100.0;
        String currency = stripeSession.getCurrency();

        String subject = messageSource.getMessage("payment.subject", null, Locale.ENGLISH);
        String message = messageSource.getMessage("payment.message",
                new Object[]{String.format("%.2f", value), currency}, Locale.ENGLISH);

        sendNotification(stripeSession.getCustomerEmail(), message, subject);
    }
}
