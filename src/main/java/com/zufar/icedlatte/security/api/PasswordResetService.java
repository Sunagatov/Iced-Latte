package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.email.exception.TimeTokenException;
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
    private final EmailTokenSender emailTokenSender;
    private final EmailTokenConformer emailTokenConformer;

    public void requestReset(String email) {
        try {
            singleUserProvider.getUserEntityByEmail(email);
            emailTokenSender.sendPasswordResetCode(email);
        } catch (UserNotFoundException _) {
            log.warn("auth.password.forgot.unknown_email");
        } catch (TimeTokenException _) {
            // Swallow cooldown error — returning a distinct response would confirm the email exists.
            log.warn("auth.password.forgot.cooldown");
        }
    }

    public void confirmReset(String token, String newPassword) {
        emailTokenConformer.confirmResetPasswordEmailByCode(new ConfirmEmailRequest(token), newPassword);
        log.info("auth.password.changed");
    }
}
