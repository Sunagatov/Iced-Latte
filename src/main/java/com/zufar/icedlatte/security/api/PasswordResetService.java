package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.exception.TimeTokenException;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final SingleUserProvider singleUserProvider;
    private final EmailVerificationService emailVerificationService;

    public void requestReset(String email) {
        try {
            singleUserProvider.getUserEntityByEmail(email);
            emailVerificationService.sendPasswordResetCode(email);
        } catch (UserNotFoundException _) {
            log.debug("auth.password.forgot.unknown_email");
        } catch (TimeTokenException _) {
            // Swallow cooldown error — returning a distinct response would confirm the email exists.
            log.debug("auth.password.forgot.cooldown");
        }
    }

    public void confirmReset(String token, String newPassword) {
        emailVerificationService.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(token), newPassword);
        log.info("auth.password.changed");
    }
}
