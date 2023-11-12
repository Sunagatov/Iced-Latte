package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailToUserOperationPerformer {

    private final JavaMailSender mailSender;
    private final UserRepository userCrudRepository;
    @Value("${iced-latte.feature.email-confirmation.enabled}")
    private boolean emailConfirmationEnabled;

    @Value("${iced-latte.feature.email-confirmation.email-properties.from}")
    private String emailFrom;

    @Value("${iced-latte.feature.email-confirmation.base-link}")
    private String confirmationBaseLink;

    public void sendUserEmailConfirmationEmail(final UUID userId) {
        log.info("Sending the confirmation email to the user with the id = {}.", userId);
        if (emailConfirmationEnabled) {
            var userEntity = userCrudRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Failed to get the user with the id = {}.", userId);
                        return new UserNotFoundException(userId);
                    });
            final var token = userEntity.getConfirmationToken();
            final var email = userEntity.getEmail();
            final var message = composeConfirmationMessage(
                    mailSender.createMimeMessage(),
                    email,
                    emailFrom,
                    token,
                    confirmationBaseLink
            );
            mailSender.send(message);
        }
    }

    private static MimeMessage composeConfirmationMessage(
            MimeMessage message,
            String emailTo,
            String emailFrom,
            String token,
            String comfirmationBaseLink) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setTo(emailTo);
            helper.setFrom(emailFrom);
            helper.setSubject("Iced Latte Email confirmation");
            helper.setText(
                    "Your confirmation code is" + token + "\n" +
                            "Please, confirm your email by clicking on the link below:\n" +
                            comfirmationBaseLink + token + "\n" +
                            "If you didn't register on our website, please, ignore this email.\n" +
                            "Iced Latte Team \uD83E\uDDCA ☕ "
            );
            return helper.getMimeMessage();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
