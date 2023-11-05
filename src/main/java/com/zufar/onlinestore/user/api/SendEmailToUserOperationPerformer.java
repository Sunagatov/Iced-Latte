package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailToUserOperationPerformer {

    private final MailSender mailSender;
    private final UserRepository userCrudRepository;
    @Value("${iced-latte.feature.email-confirmation.enabled}")
    private boolean emailConfirmationEnabled;

    public void sendUserEmailConfirmationEmail(final UUID userId) {
        if (emailConfirmationEnabled) {
            var userEntity = userCrudRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Failed to get the user with the id = {}.", userId);
                        return new UserNotFoundException(userId);
                    });
            final var token = userEntity.getConfirmationToken();
            final var email = userEntity.getEmail();
            final var message = createMailMessage(email, token);
            mailSender.send(message);
        }
    }

    private static SimpleMailMessage createMailMessage(String email, String token) {
        final var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Iced Latte Email confirmation");
        message.setText(
                "Your confirmation code is" + token + "\n" +
                        "Please, confirm your email by clicking on the link below:\n" +
                        "http://localhost:8080/api/v1/users/confirm-email/" + token + "\n" +
                        "If you didn't register on our website, please, ignore this email." +
                        "Iced Latte Team \uD83E\uDDCA ☕ "
        );
        return message;
    }
}
