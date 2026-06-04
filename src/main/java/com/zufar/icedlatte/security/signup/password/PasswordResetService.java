package com.zufar.icedlatte.security.signup.password;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.security.signup.exception.TimeTokenException;
import com.zufar.icedlatte.security.signup.verification.EmailVerificationService;
import com.zufar.icedlatte.user.api.UserLookupApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserLookupApi userLookupApi;
    private final EmailVerificationService emailVerificationService;
    private final TurnstileVerifier turnstileVerifier;

    public void requestReset(String email, String turnstileToken) {
        turnstileVerifier.verify(turnstileToken);
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

    public void confirmReset(String token, String newPassword, String turnstileToken) {
        turnstileVerifier.verify(turnstileToken);
        emailVerificationService.confirmResetPasswordEmailByCode(token, newPassword);
        log.info("auth.password.changed");
    }
}
