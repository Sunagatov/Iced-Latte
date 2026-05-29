package com.zufar.icedlatte.security.signup.password;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;
import com.zufar.icedlatte.security.signup.verification.EmailVerificationService;
import com.zufar.icedlatte.security.util.EmailNormalizer;
import com.zufar.icedlatte.user.api.UserLookupApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserLookupApi userLookupApi;
    private final EmailVerificationService emailVerificationService;

    public void requestReset(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        try {
            if (userLookupApi.findUserByEmail(normalizedEmail).isEmpty()) {
                log.debug("auth.password.forgot.unknown_email");
                return;
            }
            emailVerificationService.sendPasswordResetCode(normalizedEmail);
        } catch (TimeTokenException _) {
            // Swallow cooldown error — returning a distinct response would confirm the email exists.
            log.debug("auth.password.forgot.cooldown");
        }
    }

    public void confirmReset(String token, String newPassword) {
        ConfirmEmailRequest confirmEmailRequest = new ConfirmEmailRequest(token);
        emailVerificationService.confirmResetPasswordEmailByCode(confirmEmailRequest, newPassword);
        log.info("auth.password.changed");
    }
}
